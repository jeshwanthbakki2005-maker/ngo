package com.ngo.service;

import com.ngo.entity.*;
import com.ngo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ngo.security.PasswordPolicy;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    @Autowired
    private PasswordResetRequestRepository passwordResetRequestRepository;

    @Autowired
    private NFSARepository nfsaRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordPolicy passwordPolicy;

    // Detect the account category from the email so the user only enters one field.
    @Transactional
    public PasswordResetRequest requestPasswordReset(String email) {
        Optional<NFSA> nfsa = nfsaRepository.findByEmailIgnoreCase(email);
        if (nfsa.isPresent()) {
            return requestPasswordReset(nfsa.get().getEmail(), "nfsa");
        }

        Optional<Donor> donor = donorRepository.findByEmailIgnoreCase(email);
        if (donor.isPresent()) {
            return requestPasswordReset(donor.get().getEmail(), "donor");
        }

        Optional<Beneficiary> beneficiary = beneficiaryRepository.findByEmailIgnoreCase(email);
        if (beneficiary.isPresent()) {
            return requestPasswordReset(beneficiary.get().getEmail(), "beneficiary");
        }

        throw new RuntimeException("Invalid email");
    }
    // Create a short-lived Gmail OTP before issuing the password-reset token.
    @Transactional
    public PasswordResetRequest requestPasswordReset(String email, String userType) {
        User user = findUserByEmail(email, userType);
        if (user == null) {
            throw new RuntimeException("Invalid email");
        }

        LocalDateTime now = LocalDateTime.now();
        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
        PasswordResetRequest resetReq = passwordResetRequestRepository.findByEmail(email)
                .orElseGet(PasswordResetRequest::new);
        resetReq.setUserId(user.getId());
        resetReq.setUserType(userType);
        resetReq.setEmail(email);
        resetReq.setStatus("otp_pending");
        resetReq.setToken(null);
        resetReq.setTokenGeneratedAt(null);
        resetReq.setApprovedAt(null);
        resetReq.setCompletedAt(null);
        resetReq.setCreatedAt(now);
        resetReq.setOtpHash(passwordEncoder.encode(otp));
        resetReq.setOtpExpiresAt(now.plusMinutes(10));
        resetReq.setOtpVerifiedAt(null);
        resetReq.setOtpAttempts(0);

        logAction("PASSWORD_RESET_OTP_REQUEST", userType, user.getId(), "Password reset OTP generated for " + email);

        PasswordResetRequest savedRequest = passwordResetRequestRepository.save(resetReq);
        savedRequest.setOtpCode(otp);
        return savedRequest;
    }
    // Generate reset token (called after NFSA approval)
    @Transactional
    public String generateResetToken(Long requestId) {
        PasswordResetRequest resetReq = passwordResetRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Reset request not found"));

        if (!"approved".equals(resetReq.getStatus())) {
            throw new RuntimeException("Reset request must be approved first");
        }

        String token = UUID.randomUUID().toString();
        resetReq.setToken(token);
        resetReq.setTokenGeneratedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(resetReq);

        return token;
    }

    // Complete password reset
    @Transactional
    public void completePasswordReset(String token, String newPassword) {
        passwordPolicy.validate(newPassword);

        PasswordResetRequest resetReq = findValidResetRequest(token);

        // Update user password
        String hashedPassword = passwordEncoder.encode(newPassword);
        updateUserPassword(resetReq.getUserId(), resetReq.getUserType(), hashedPassword);

        // Mark reset as completed
        resetReq.setStatus("completed");
        resetReq.setCompletedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(resetReq);

        logAction("PASSWORD_RESET_COMPLETED", resetReq.getUserType(), resetReq.getUserId(), 
                  "Password reset successfully");
    }

    public boolean hasActiveOtp(String email) {
        return email != null && passwordResetRequestRepository.findByEmail(email)
                .filter(request -> "otp_pending".equals(request.getStatus()))
                .filter(request -> request.getOtpExpiresAt() != null
                        && request.getOtpExpiresAt().isAfter(LocalDateTime.now()))
                .filter(request -> request.getOtpAttempts() != null && request.getOtpAttempts() < 5)
                .isPresent();
    }

    public PasswordResetRequest verifyOtp(String email, String otp) {
        PasswordResetRequest request = passwordResetRequestRepository.findByEmail(email)
                .filter(item -> "otp_pending".equals(item.getStatus()))
                .orElseThrow(() -> new RuntimeException("No active OTP request was found."));

        if (request.getOtpExpiresAt() == null || !request.getOtpExpiresAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new OTP.");
        }

        int attempts = request.getOtpAttempts() == null ? 0 : request.getOtpAttempts();
        if (attempts >= 5) {
            throw new RuntimeException("Too many incorrect attempts. Please request a new OTP.");
        }

        if (otp == null || !otp.matches("^[0-9]{6}$")
                || request.getOtpHash() == null || !passwordEncoder.matches(otp, request.getOtpHash())) {
            request.setOtpAttempts(attempts + 1);
            passwordResetRequestRepository.save(request);
            throw new RuntimeException(attempts + 1 >= 5
                    ? "Too many incorrect attempts. Please request a new OTP."
                    : "Invalid OTP.");
        }

        LocalDateTime now = LocalDateTime.now();
        request.setStatus("approved");
        request.setToken(UUID.randomUUID().toString());
        request.setTokenGeneratedAt(now);
        request.setApprovedAt(now);
        request.setOtpVerifiedAt(now);
        request.setOtpHash(null);
        request.setOtpExpiresAt(null);
        request.setOtpAttempts(0);
        return passwordResetRequestRepository.save(request);
    }
    public String getAccountName(PasswordResetRequest request) {
        if (request == null || request.getUserId() == null) {
            return "Account Holder";
        }
        if ("nfsa".equalsIgnoreCase(request.getUserType())) {
            return nfsaRepository.findById(request.getUserId())
                    .map(NFSA::getFullName).orElse("Account Holder");
        }
        if ("donor".equalsIgnoreCase(request.getUserType())) {
            return donorRepository.findById(request.getUserId())
                    .map(Donor::getFullName).orElse("Account Holder");
        }
        if ("beneficiary".equalsIgnoreCase(request.getUserType())) {
            return beneficiaryRepository.findById(request.getUserId())
                    .map(Beneficiary::getFullName).orElse("Account Holder");
        }
        return "Account Holder";
    }
    public boolean isValidResetToken(String token) {
        try {
            findValidResetRequest(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private PasswordResetRequest findValidResetRequest(String token) {
        return passwordResetRequestRepository.findByTokenAndStatus(token, "approved")
                .filter(request -> request.getTokenGeneratedAt() != null
                        && request.getTokenGeneratedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));
    }
    private User findUserByEmail(String email, String userType) {
        if ("nfsa".equalsIgnoreCase(userType)) {
            Optional<NFSA> nfsa = nfsaRepository.findByEmailIgnoreCase(email);
            return nfsa.isPresent() ? new UserAdapter(nfsa.get()) : null;
        } else if ("donor".equalsIgnoreCase(userType)) {
            Optional<Donor> donor = donorRepository.findByEmailIgnoreCase(email);
            return donor.isPresent() ? new UserAdapter(donor.get()) : null;
        } else if ("beneficiary".equalsIgnoreCase(userType)) {
            Optional<Beneficiary> beneficiary = beneficiaryRepository.findByEmailIgnoreCase(email);
            return beneficiary.isPresent() ? new UserAdapter(beneficiary.get()) : null;
        }
        return null;
    }

    private void updateUserPassword(Long userId, String userType, String hashedPassword) {
        if ("nfsa".equalsIgnoreCase(userType)) {
            NFSA nfsa = nfsaRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("NFSA not found"));
            nfsa.setPasswordHash(hashedPassword);
            nfsaRepository.save(nfsa);
        } else if ("donor".equalsIgnoreCase(userType)) {
            Donor donor = donorRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Donor not found"));
            donor.setPasswordHash(hashedPassword);
            donorRepository.save(donor);
        } else if ("beneficiary".equalsIgnoreCase(userType)) {
            Beneficiary beneficiary = beneficiaryRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Beneficiary not found"));
            beneficiary.setPasswordHash(hashedPassword);
            beneficiaryRepository.save(beneficiary);
        }
    }

    private void logAction(String action, String userType, Long userId, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUserType(userType);
        log.setUserId(userId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    // Interface for user adapter
    private interface User {
        Long getId();
    }

    // Adapter class to handle different user types
    private static class UserAdapter implements User {
        private NFSA nfsa;
        private Donor donor;
        private Beneficiary beneficiary;

        UserAdapter(NFSA nfsa) {
            this.nfsa = nfsa;
        }

        UserAdapter(Donor donor) {
            this.donor = donor;
        }

        UserAdapter(Beneficiary beneficiary) {
            this.beneficiary = beneficiary;
        }

        @Override
        public Long getId() {
            if (nfsa != null) return nfsa.getId();
            if (donor != null) return donor.getId();
            if (beneficiary != null) return beneficiary.getId();
            return null;
        }
    }
}
