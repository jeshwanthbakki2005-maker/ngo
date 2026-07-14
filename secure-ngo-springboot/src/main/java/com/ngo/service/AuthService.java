package com.ngo.service;

import com.ngo.dto.LoginRequest;
import com.ngo.dto.LoginResponse;
import com.ngo.dto.SignupRequest;
import com.ngo.entity.*;
import com.ngo.repository.*;
import com.ngo.security.JwtUtil;
import com.ngo.security.PasswordPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

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
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordPolicy passwordPolicy;

    // NFSA Login
    public LoginResponse nfsaLogin(LoginRequest request) {
        Optional<NFSA> nfsaOpt = nfsaRepository.findByEmail(request.getEmail());
        
        if (nfsaOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), nfsaOpt.get().getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        NFSA nfsa = nfsaOpt.get();
        
        // Create JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", nfsa.getId());
        claims.put("role", "nfsa");
        claims.put("name", nfsa.getFullName());
        
        String token = jwtUtil.generateToken(claims, nfsa.getEmail());
        
        // Log action
        logAction("NFSA_LOGIN", "nfsa", nfsa.getId(), "Login successful");
        
        return new LoginResponse(token, nfsa.getId(), nfsa.getFullName(), "nfsa", nfsa.getEmail());
    }

    // NFSA Signup
    public LoginResponse nfsaSignup(NFSA nfsa) {
        passwordPolicy.validate(nfsa.getPasswordHash());
        if (nfsaRepository.findByEmail(nfsa.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        nfsa.setPasswordHash(passwordEncoder.encode(nfsa.getPasswordHash()));
        nfsa.setCreatedAt(LocalDateTime.now());
        nfsa.setIsVerified(true);
        
        NFSA saved = nfsaRepository.save(nfsa);
        logAction("NFSA_SIGNUP", "nfsa", saved.getId(), "New NFSA registered: " + nfsa.getEmail());
        
        // Generate token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", saved.getId());
        claims.put("role", "nfsa");
        claims.put("name", saved.getFullName());
        
        String token = jwtUtil.generateToken(claims, saved.getEmail());
        
        return new LoginResponse(token, saved.getId(), saved.getFullName(), "nfsa", saved.getEmail());
    }

    // Donor Login
    public LoginResponse donorLogin(LoginRequest request) {
        Optional<Donor> donorOpt = donorRepository.findByEmail(request.getEmail());
        
        if (donorOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), donorOpt.get().getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        Donor donor = donorOpt.get();
        if (!Boolean.TRUE.equals(donor.getIsVerified())) {
            donor.setIsVerified(true);
            donor = donorRepository.save(donor);
        }
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", donor.getId());
        claims.put("role", "donor");
        claims.put("name", donor.getFullName());
        claims.put("verified", donor.getIsVerified());
        
        String token = jwtUtil.generateToken(claims, donor.getEmail());
        logAction("DONOR_LOGIN", "donor", donor.getId(), "Login successful");
        
        return new LoginResponse(token, donor.getId(), donor.getFullName(), "donor", donor.getEmail());
    }

    // Donor Signup
    public LoginResponse donorSignup(Donor donor) {
        passwordPolicy.validate(donor.getPasswordHash());
        if (donorRepository.findByEmail(donor.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        donor.setPasswordHash(passwordEncoder.encode(donor.getPasswordHash()));
        donor.setCreatedAt(LocalDateTime.now());
        donor.setIsVerified(true);
        
        Donor saved = donorRepository.save(donor);
        logAction("DONOR_SIGNUP", "donor", saved.getId(), "New donor registered: " + donor.getEmail());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", saved.getId());
        claims.put("role", "donor");
        claims.put("name", saved.getFullName());
        claims.put("verified", saved.getIsVerified());
        
        String token = jwtUtil.generateToken(claims, saved.getEmail());
        
        return new LoginResponse(token, saved.getId(), saved.getFullName(), "donor", saved.getEmail());
    }

    // Beneficiary Login
    public LoginResponse beneficiaryLogin(LoginRequest request) {
        Optional<Beneficiary> beneficiaryOpt = beneficiaryRepository.findByEmail(request.getEmail());
        
        if (beneficiaryOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), beneficiaryOpt.get().getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        Beneficiary beneficiary = beneficiaryOpt.get();
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", beneficiary.getId());
        claims.put("role", "beneficiary");
        claims.put("name", beneficiary.getFullName());
        claims.put("verified", beneficiary.getIsVerified());
        
        String token = jwtUtil.generateToken(claims, beneficiary.getEmail());
        logAction("BENEFICIARY_LOGIN", "beneficiary", beneficiary.getId(), "Login successful");
        
        return new LoginResponse(token, beneficiary.getId(), beneficiary.getFullName(), "beneficiary", beneficiary.getEmail());
    }

    // Beneficiary Signup
    public LoginResponse beneficiarySignup(Beneficiary beneficiary) {
        passwordPolicy.validate(beneficiary.getPasswordHash());
        if (beneficiaryRepository.findByEmail(beneficiary.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        beneficiary.setPasswordHash(passwordEncoder.encode(beneficiary.getPasswordHash()));
        beneficiary.setCreatedAt(LocalDateTime.now());
        beneficiary.setIsVerified(false);
        
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        logAction("BENEFICIARY_SIGNUP", "beneficiary", saved.getId(), "New beneficiary registered: " + beneficiary.getEmail());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", saved.getId());
        claims.put("role", "beneficiary");
        claims.put("name", saved.getFullName());
        claims.put("verified", saved.getIsVerified());
        
        String token = jwtUtil.generateToken(claims, saved.getEmail());
        
        return new LoginResponse(token, saved.getId(), saved.getFullName(), "beneficiary", saved.getEmail());
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
}
