package com.ngo.controller;

import com.ngo.dto.LoginRequest;
import com.ngo.dto.LoginResponse;
import com.ngo.entity.Beneficiary;
import com.ngo.entity.DisasterReport;
import com.ngo.entity.Donor;
import com.ngo.entity.NFSA;
import com.ngo.repository.DisasterReportRepository;
import com.ngo.repository.DonorRepository;
import com.ngo.repository.NFSARepository;
import com.ngo.repository.AllocationRepository;
import com.ngo.repository.UtilizationReportRepository;
import com.ngo.service.AuthService;
import com.ngo.service.BeneficiaryService;
import com.ngo.service.DonorService;
import com.ngo.service.NFSAService;
import com.ngo.security.JwtDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.ngo.entity.Donation;
import com.ngo.entity.Allocation;

@Controller
public class WebController {

    @Autowired
    private AuthService authService;

    @Autowired
    private NFSAService nfsaService;

    @Autowired
    private BeneficiaryService beneficiaryService;

    @Autowired
    private DonorService donorService;

    @Autowired
    private DisasterReportRepository disasterReportRepository;

    @Autowired
    private com.ngo.repository.DonationRepository donationRepository;

    @Autowired
    private com.ngo.repository.BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private NFSARepository nfsaRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private UtilizationReportRepository utilizationReportRepository;

    @Autowired
    private com.ngo.repository.ReliefMaterialRequestRepository reliefMaterialRequestRepository;

    @Autowired
    private com.ngo.repository.ReliefMaterialDonationRepository reliefMaterialDonationRepository;

    @Autowired
    private com.ngo.service.EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.ngo.service.PasswordResetService passwordResetService;

    @Value("${app.frontend-url:http://localhost:5000}")
    private String frontendUrl;

    private List<Map<String, Object>> buildNfsaNotifications() {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        disasterReportRepository.findAllByOrderByCreatedAtDesc().forEach(report -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", "Disaster report #" + report.getId());
            item.put("message", report.getDisasterType() + " in " + report.getLocation() + " is " + report.getStatus() + ".");
            item.put("date", report.getCreatedAt()); item.put("type", "disaster"); item.put("link", "/nfsa/campaigns"); items.add(item);
        });
        donationRepository.findTop5ByOrderByCreatedAtDesc().forEach(donation -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", "Donation received");
            item.put("message", "₹" + String.format("%,.0f", donation.getAmount()) + " received for " + donation.getPurpose() + ".");
            item.put("date", donation.getCreatedAt()); item.put("type", "donation"); item.put("link", "/nfsa/donations"); items.add(item);
        });
        items.sort((a,b) -> ((LocalDateTime)b.get("date")).compareTo((LocalDateTime)a.get("date")));
        return items;
    }

    @ModelAttribute
    public void addNfsaHeaderNotifications(HttpServletRequest request, Model model) {
        if (!request.getRequestURI().startsWith("/nfsa/")) return;
        Long currentAdminId = getUserId();
        if (currentAdminId != null) {
            nfsaRepository.findById(currentAdminId).ifPresent(admin -> {
                model.addAttribute("name", admin.getFullName());
                model.addAttribute("adminRole", admin.getDesignation());
                model.addAttribute("adminOrganization", admin.getOrganizationName());
            });
        }
        List<Map<String, Object>> notifications = buildNfsaNotifications();
        model.addAttribute("headerNotifications", notifications.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("headerNotificationCount", notifications.size());
        model.addAttribute("headerMessageCount", Math.min(notifications.size(), 9));
    }

    private List<Map<String, Object>> buildBeneficiaryNotifications(Long beneficiaryId) {
        List<Map<String, Object>> notifications = new java.util.ArrayList<>();
        beneficiaryRepository.findById(beneficiaryId).ifPresent(beneficiary -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", Boolean.TRUE.equals(beneficiary.getIsVerified()) ? "Account verified" : "Verification pending");
            item.put("message", Boolean.TRUE.equals(beneficiary.getIsVerified())
                    ? "Your beneficiary account is verified and eligible to receive support."
                    : "Your beneficiary account is awaiting administrator verification.");
            item.put("date", beneficiary.getCreatedAt()); item.put("type", "account"); item.put("link", "/beneficiary/profile");
            notifications.add(item);
        });
        disasterReportRepository.findByReportedByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId).forEach(report -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", "Disaster report #" + report.getId());
            item.put("message", "Your " + report.getDisasterType() + " report status is "
                    + (report.getStatus() == null ? "pending" : report.getStatus()) + ".");
            item.put("date", report.getCreatedAt()); item.put("type", "disaster"); item.put("link", "/beneficiary/campaigns");
            notifications.add(item);
        });
        reliefMaterialRequestRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId).forEach(request -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("title", "Material request #" + request.getId());
            item.put("message", "Your " + request.getMaterialType() + " request is " + request.getStatus() + ".");
            item.put("date", request.getUpdatedAt() != null ? request.getUpdatedAt() : request.getCreatedAt());
            item.put("type", "material"); item.put("link", "/beneficiary/relief-materials"); notifications.add(item);
        });
        notifications.sort((a, b) -> ((LocalDateTime) b.get("date")).compareTo((LocalDateTime) a.get("date")));
        return notifications;
    }

    private List<Map<String, Object>> buildDonorNotifications(Long donorId) {
        List<Map<String, Object>> notifications = new java.util.ArrayList<>();
        disasterReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(report -> report.getStatus() != null && !"rejected".equalsIgnoreCase(report.getStatus().trim()))
                .forEach(report -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    item.put("title", "Campaign: " + report.getDisasterType() + " in " + report.getLocation());
                    item.put("message", "Status: " + report.getStatus() + ". " + (report.getDescription() == null ? "Emergency support is required." : report.getDescription()));
                    item.put("date", report.getCreatedAt()); item.put("type", "campaign"); item.put("link", "/kk/campaigns"); notifications.add(item);
                });
        donationRepository.findByDonorIdOrderByCreatedAtDesc(donorId).forEach(donation -> {
            Map<String, Object> item = new java.util.HashMap<>(); item.put("title", "Donation completed");
            item.put("message", "Your donation of ₹" + String.format("%,.0f", donation.getAmount()) + " for " + donation.getPurpose() + " was recorded.");
            item.put("date", donation.getCreatedAt()); item.put("type", "donation"); item.put("link", "/kk/history"); notifications.add(item);
        });
        notifications.sort((a,b) -> ((LocalDateTime)b.get("date")).compareTo((LocalDateTime)a.get("date")));
        return notifications;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void addJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private Long getUserId() {
        try {
            return Long.parseLong((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserName() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtDetails) {
                return ((JwtDetails) auth.getDetails()).getName();
            }
        } catch (Exception ignored) {}
        return "User";
    }

    private String getUserRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getDetails() instanceof JwtDetails) {
                return ((JwtDetails) auth.getDetails()).getRole();
            }
        } catch (Exception ignored) {}
        return null;
    }


    // ─── Home ─────────────────────────────────────────────────────────────────

    @GetMapping("/auth/logout")
    public String webLogout(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/auth/login")
    public String unifiedLogin(@RequestParam String email, @RequestParam String password,
                               HttpServletResponse response, Model model) {
        String enteredEmail = email == null ? "" : email.trim();
        try {
            LoginResponse login;
            String destination;
            var nfsa = nfsaRepository.findByEmailIgnoreCase(enteredEmail);
            var donor = donorRepository.findByEmailIgnoreCase(enteredEmail);
            var beneficiary = beneficiaryRepository.findByEmailIgnoreCase(enteredEmail);
            if (nfsa.isPresent()) {
                login = authService.nfsaLogin(new LoginRequest(nfsa.get().getEmail(), password)); destination = "/nfsa/dashboard";
            } else if (donor.isPresent()) {
                login = authService.donorLogin(new LoginRequest(donor.get().getEmail(), password)); destination = "/kk/donor";
            } else if (beneficiary.isPresent()) {
                login = authService.beneficiaryLogin(new LoginRequest(beneficiary.get().getEmail(), password)); destination = "/beneficiary/dashboard";
            } else {
                throw new IllegalArgumentException("No registered account was found for this email address.");
            }
            addJwtCookie(response, login.getToken());
            return "redirect:" + destination;
        } catch (Exception exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("enteredEmail", enteredEmail);
            return "home";
        }
    }

    // ─── NFSA Auth ─────────────────────────────────────────────────────────────

    @GetMapping("/auth/nfsa/signup")
    public String nfsaSignupPage(@RequestParam(value = "accountDeleted", required = false) String accountDeleted,
                                 Model model) {
        model.addAttribute("error", false);
        if ("true".equalsIgnoreCase(accountDeleted)) {
            model.addAttribute("success", "The previous administrator account and all platform data were deleted. You can now register with a completely fresh system.");
        }
        return "nfsa/signup";
    }

    @PostMapping("/auth/nfsa/signup")
    public String handleNfsaSignup(
            @RequestParam("full_name") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("organization_name") String orgName,
            @RequestParam("designation") String designation,
            @RequestParam("phone") String phone,
            @RequestParam("employee_id") String empId,
            HttpServletResponse response, Model model) {
        try {
            NFSA nfsa = new NFSA(fullName, email, password, orgName, designation, phone, empId);
            LoginResponse lr = authService.nfsaSignup(nfsa);
            addJwtCookie(response, lr.getToken());
            return "redirect:/nfsa/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "nfsa/signup";
        }
    }

    @GetMapping("/auth/nfsa/login")
    public String nfsaLoginPage(Model model) {
        model.addAttribute("error", false);
        return "nfsa/login";
    }

    @PostMapping("/auth/nfsa/login")
    public String handleNfsaLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletResponse response, Model model) {
        try {
            LoginResponse lr = authService.nfsaLogin(new LoginRequest(email, password));
            addJwtCookie(response, lr.getToken());
            return "redirect:/nfsa/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "nfsa/login";
        }
    }

    // ─── Beneficiary Auth ──────────────────────────────────────────────────────

    @GetMapping("/auth/beneficiary/signup")
    public String beneficiarySignupPage(Model model) {
        model.addAttribute("error", false);
        return "beneficiary/signup";
    }

    @PostMapping("/auth/beneficiary/signup")
    public String handleBeneficiarySignup(
            @RequestParam("full_name") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("need_category") String needCategory,
            @RequestParam("phone") String phone,
            @RequestParam("address") String address,
            @RequestParam(value = "family_members", defaultValue = "1") Integer familyMembers,
            @RequestParam(value = "monthly_income", defaultValue = "0") Double monthlyIncome,
            HttpServletResponse response, Model model) {
        try {
            Beneficiary beneficiary = new Beneficiary(fullName, email, password, phone, address, needCategory);
            beneficiary.setFamilyMembers(familyMembers);
            beneficiary.setMonthlyIncome(monthlyIncome);
            LoginResponse lr = authService.beneficiarySignup(beneficiary);
            addJwtCookie(response, lr.getToken());
            return "redirect:/beneficiary/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "beneficiary/signup";
        }
    }

    @GetMapping("/auth/beneficiary/login")
    public String beneficiaryLoginPage(Model model) {
        model.addAttribute("error", false);
        return "beneficiary/login";
    }

    @PostMapping("/auth/beneficiary/login")
    public String handleBeneficiaryLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletResponse response, Model model) {
        try {
            LoginResponse lr = authService.beneficiaryLogin(new LoginRequest(email, password));
            addJwtCookie(response, lr.getToken());
            return "redirect:/beneficiary/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "beneficiary/login";
        }
    }

    // ─── KK Auth ────────────────────────────────────────────────────────────

    @GetMapping({"/kk/signup", "/kk/signup.html"})
    public String kkSignupPage(Model model) {
        model.addAttribute("error", false);
        return "kk/signup";
    }

    @PostMapping("/kk/signup")
    public String handleKkSignup(
            @RequestParam("full_name") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phone") String phone,
            @RequestParam(value = "address", required = false, defaultValue = "") String address,
            @RequestParam(value = "organization", required = false, defaultValue = "") String organization,
            @RequestParam(value = "city", required = false, defaultValue = "") String city,
            HttpServletResponse response, Model model) {
        try {
            Donor donor = new Donor(fullName, email, password, phone, address);
            LoginResponse lr = authService.donorSignup(donor);
            addJwtCookie(response, lr.getToken());
            return "redirect:/kk/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "kk/signup";
        }
    }

    @GetMapping({"/kk/login", "/kk/login.html"})
    public String kkLoginPage(
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {
        model.addAttribute("error", false);
        if ("true".equalsIgnoreCase(registered)) {
            model.addAttribute("success", "Registration successful. Please log in.");
        }
        return "kk/login";
    }
    @GetMapping("/kk/dashboard")
    public String kkDashboardRedirect() {
        return "redirect:/kk/donor";
    }

    @GetMapping({"/kk/donor", "/kk/donor.html"})
    public String kkDonorPage(Model model) {
        Long userId = getUserId();
        if (userId == null) {
            return "redirect:/kk/login";
        }

        try {
            donorRepository.findById(userId).ifPresent(existingDonor -> {
                if (!Boolean.TRUE.equals(existingDonor.getIsVerified())) {
                    existingDonor.setIsVerified(true);
                    donorRepository.save(existingDonor);
                }
            });
            Map<String, Object> data = donorService.getDonorDashboard(userId);
            model.addAttribute("donor", data.get("donor"));
            model.addAttribute("total_donated", data.get("totalDonated"));
            model.addAttribute("donation_count", data.get("donationCount"));
            model.addAttribute("recentDonations", data.get("recentDonations"));
            model.addAttribute("activePage", "dashboard");

            List<Donation> donorDonations = donationRepository.findByDonorIdOrderByCreatedAtDesc(userId);
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            double thisMonthTotal = donorDonations.stream()
                    .filter(d -> d.getCreatedAt() != null && java.time.YearMonth.from(d.getCreatedAt()).equals(currentMonth))
                    .mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum();
            long thisMonthCount = donorDonations.stream()
                    .filter(d -> d.getCreatedAt() != null && java.time.YearMonth.from(d.getCreatedAt()).equals(currentMonth)).count();
            model.addAttribute("thisMonthTotal", thisMonthTotal);
            model.addAttribute("thisMonthCount", thisMonthCount);

            int year = java.time.LocalDate.now().getYear();
            List<Double> monthlyDonations = new java.util.ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                final int selectedMonth = month;
                monthlyDonations.add(donorDonations.stream()
                        .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().getYear() == year
                                && d.getCreatedAt().getMonthValue() == selectedMonth)
                        .mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum());
            }
            model.addAttribute("monthlyDonations", monthlyDonations);

            Map<String, Double> purposeTotals = donorDonations.stream().collect(Collectors.groupingBy(
                    d -> d.getPurpose() == null || d.getPurpose().isBlank() ? "General Fund" : d.getPurpose(),
                    java.util.LinkedHashMap::new,
                    Collectors.summingDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0)));
            model.addAttribute("purposeLabels", new java.util.ArrayList<>(purposeTotals.keySet()));
            model.addAttribute("purposeValues", new java.util.ArrayList<>(purposeTotals.values()));

            Map<Long, Double> totalsByDonor = donationRepository.findAllByOrderByCreatedAtDesc().stream()
                    .collect(Collectors.groupingBy(Donation::getDonorId, Collectors.summingDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0)));
            List<Map<String, Object>> topDonors = totalsByDonor.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).limit(5).map(entry -> {
                        Map<String, Object> row = new java.util.HashMap<>();
                        row.put("name", donorRepository.findById(entry.getKey()).map(Donor::getFullName).orElse("Unknown Donor"));
                        row.put("amount", entry.getValue());
                        return row;
                    }).collect(Collectors.toList());
            model.addAttribute("topDonors", topDonors);

            List<com.ngo.entity.ReliefMaterialDonation> recentMaterialDonations =
                    reliefMaterialDonationRepository.findByDonorIdOrderByCreatedAtDesc(userId);
            model.addAttribute("materialDonationCount", recentMaterialDonations.size());
            model.addAttribute("totalContributionCount", donorDonations.size() + recentMaterialDonations.size());
            if (recentMaterialDonations.size() > 5) recentMaterialDonations = recentMaterialDonations.subList(0, 5);
            recentMaterialDonations.forEach(item -> reliefMaterialRequestRepository.findById(item.getRequestId())
                    .ifPresent(request -> item.setMaterialType(request.getMaterialType())));
            model.addAttribute("recentMaterialDonations", recentMaterialDonations);

            List<Map<String, Object>> recentContributions = new java.util.ArrayList<>();
            donorDonations.forEach(donation -> {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("type", "fund");
                item.put("title", donation.getPurpose() == null ? "General Fund" : donation.getPurpose());
                item.put("value", "₹" + String.format("%,.0f", donation.getAmount() == null ? 0.0 : donation.getAmount()));
                item.put("date", donation.getCreatedAt());
                recentContributions.add(item);
            });
            reliefMaterialDonationRepository.findByDonorIdOrderByCreatedAtDesc(userId).forEach(material -> {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("type", "material");
                String materialName = reliefMaterialRequestRepository.findById(material.getRequestId())
                        .map(com.ngo.entity.ReliefMaterialRequest::getMaterialType).orElse("Material");
                item.put("title", "Request #" + material.getRequestId() + " - " + materialName);
                item.put("value", material.getQuantity() + " item(s)");
                item.put("date", material.getCreatedAt());
                recentContributions.add(item);
            });
            recentContributions.sort((a, b) -> ((LocalDateTime) b.get("date")).compareTo((LocalDateTime) a.get("date")));
            List<Map<String, Object>> dashboardContributions = recentContributions.size() > 5
                    ? new java.util.ArrayList<>(recentContributions.subList(0, 5))
                    : recentContributions;
            model.addAttribute("recentContributions", dashboardContributions);

            List<DisasterReport> activeCampaigns = disasterReportRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(r -> r.getStatus() != null && "active".equalsIgnoreCase(r.getStatus().trim()))
                    .collect(Collectors.toList());
            activeCampaigns.forEach(report -> {
                Double raised = donationRepository.getTotalByPurpose(report.getDisasterType());
                report.setAmountRaised(raised != null ? raised : 0.0);
            });
            model.addAttribute("activeCampaignsCount", activeCampaigns.size());
            model.addAttribute("ongoingDisasters", activeCampaigns.size() > 6 ? activeCampaigns.subList(0, 6) : activeCampaigns);
            model.addAttribute("headerNotificationCount", activeCampaigns.size());
            model.addAttribute("headerMessageCount", activeCampaigns.size());
            List<Map<String, Object>> donorNotifications = buildDonorNotifications(userId);
            model.addAttribute("headerNotificationCount", donorNotifications.size());
            model.addAttribute("headerNotifications", donorNotifications.size() > 5 ? donorNotifications.subList(0, 5) : donorNotifications);
            
            // Fetch Recent Beneficiary Requests (Disaster Reports)
            java.util.List<com.ngo.entity.DisasterReport> recentRequests = disasterReportRepository.findAllByOrderByCreatedAtDesc();
            if (recentRequests.size() > 5) recentRequests = recentRequests.subList(0, 5);
            model.addAttribute("beneficiaryRequests", recentRequests);

            return "kk/donor";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "kk/login";
        }
    }

    private String renderKkPage(String page, Model model) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        try {
            Map<String, Object> data = donorService.getDonorDashboard(userId);
            model.addAttribute("donor", data.get("donor"));
            model.addAttribute("activePage", page);
            return "kk/" + page;
        } catch (Exception e) {
            return "redirect:/kk/login";
        }
    }

    @GetMapping("/kk/donate")
    public String kkDonate(Model model) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        try {
            Map<String, Object> data = donorService.getDonorDashboard(userId);
            model.addAttribute("donor", data.get("donor"));
            model.addAttribute("activePage", "donate");
            // Only administrator-approved reports become donor-facing urgent campaigns.
            java.util.List<com.ngo.entity.DisasterReport> reports = disasterReportRepository
                    .findAllByOrderByCreatedAtDesc().stream()
                    .filter(report -> report.getStatus() != null
                            && "active".equalsIgnoreCase(report.getStatus().trim()))
                    .collect(Collectors.toList());
            for (com.ngo.entity.DisasterReport r : reports) {
                Double total = donationRepository.getTotalByPurpose(r.getDisasterType());
                r.setAmountRaised(total != null ? total : 0.0);
            }
            model.addAttribute("disasterReports", reports);
            List<com.ngo.entity.ReliefMaterialRequest> materialRequests = reliefMaterialRequestRepository
                    .findAllByOrderByCreatedAtDesc().stream()
                    .filter(request -> request.getStatus() != null
                            && "approved".equalsIgnoreCase(request.getStatus().trim()))
                    .collect(Collectors.toList());
            materialRequests.forEach(request -> {
                beneficiaryRepository.findById(request.getBeneficiaryId())
                        .ifPresent(beneficiary -> request.setBeneficiaryName(beneficiary.getFullName()));
                request.setDonatedQuantity(reliefMaterialDonationRepository.getDonatedQuantity(request.getId()));
            });
            materialRequests.removeIf(request -> request.getDonatedQuantity() >= request.getQuantity());
            model.addAttribute("reliefMaterialRequests", materialRequests);
            model.addAttribute("urgentCampaignCount", reports.size() + materialRequests.size());
            return "kk/donate";
        } catch (Exception e) {
            return "redirect:/kk/login";
        }
    }

    @PostMapping("/kk/donate/submit")
    public String submitDirectDonation(
            @RequestParam Double amount,
            @RequestParam String purpose,
            @RequestParam String paymentMethod,
            RedirectAttributes ra) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        try {
            com.ngo.dto.DonationRequest req = new com.ngo.dto.DonationRequest();
            req.setAmount(amount);
            req.setPurpose(purpose);
            req.setPaymentMethod(paymentMethod);
            donorService.makeDonation(req, userId);
            ra.addFlashAttribute("successMessage", "Thank you! Your donation of ₹" + amount + " for " + purpose + " was successful.");
            return "redirect:/kk/history";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/kk/donate";
        }
    }
    @GetMapping("/kk/history")
    public String kkHistory(Model model) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        try {
            Map<String, Object> data = donorService.getDonorDashboard(userId);
            model.addAttribute("donor", data.get("donor"));
            model.addAttribute("activePage", "history");
            // Pass simple flat list of donations
            java.util.List<com.ngo.entity.Donation> donations = donationRepository.findByDonorIdOrderByCreatedAtDesc(userId);
            model.addAttribute("donations", donations);
            List<com.ngo.entity.ReliefMaterialDonation> materialDonations =
                    reliefMaterialDonationRepository.findByDonorIdOrderByCreatedAtDesc(userId);
            materialDonations.forEach(materialDonation -> reliefMaterialRequestRepository
                    .findById(materialDonation.getRequestId()).ifPresent(request -> {
                        materialDonation.setMaterialType(request.getMaterialType());
                        beneficiaryRepository.findById(request.getBeneficiaryId())
                                .ifPresent(beneficiary -> materialDonation.setBeneficiaryName(beneficiary.getFullName()));
                    }));
            model.addAttribute("materialDonations", materialDonations);
            model.addAttribute("totalMaterialItems", materialDonations.stream()
                    .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum());
            // Total donated sum
            double total = donations.stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0).sum();
            model.addAttribute("totalDonated", total);
            return "kk/history";
        } catch (Exception e) {
            return "redirect:/kk/login";
        }
    }
    @GetMapping("/kk/beneficiaries")
    public String kkBeneficiaries(Model model) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        try {
            Map<String, Object> data = donorService.getDonorDashboard(userId);
            model.addAttribute("donor", data.get("donor"));
            model.addAttribute("activePage", "beneficiaries");
            java.util.List<com.ngo.entity.Beneficiary> beneficiaries = beneficiaryRepository.findAllByOrderByCreatedAtDesc();
            model.addAttribute("beneficiaries", beneficiaries);
            model.addAttribute("totalBeneficiaries", beneficiaries.size());
            model.addAttribute("verifiedCount", beneficiaries.stream().filter(b -> Boolean.TRUE.equals(b.getIsVerified())).count());
            return "kk/beneficiaries";
        } catch (Exception e) {
            return "redirect:/kk/login";
        }
    }
    @GetMapping("/kk/reports")
    public String kkReports(Model model) {
        return renderKkPage("reports", model);
    }

    @GetMapping("/kk/reports/download/{type}")
    public ResponseEntity<byte[]> downloadImpactReport(@PathVariable String type) {
        String reportName;
        String content;
        if ("monthly".equalsIgnoreCase(type)) {
            reportName = "Monthly_Impact_Report.txt";
            content = "Monthly Impact Report\n\nYour contributions are transforming lives through education, healthcare, and relief support.\n\nHighlights:\n- Total donations received: ₹4,000\n- Donations completed: 4\n- Active campaigns: 0\n- Top supported cause: flood\n\nThank you for supporting our mission.";
        } else if ("quarterly".equalsIgnoreCase(type)) {
            reportName = "Quarterly_Impact_Report.txt";
            content = "Quarterly Impact Report\n\nThis quarter, your funds were used to support flood relief, beneficiary requests, and verified utilization.\n\nHighlights:\n- Monthly improvement: +12% in donations\n- Completed fund distribution: 100% of verified allocations\n- Beneficiary requests reviewed and prioritized\n\nThank you for helping create lasting impact.";
        } else {
            return ResponseEntity.badRequest().build();
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping("/kk/campaigns") 
    public String kkCampaigns(Model model) { 
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        try {
            Map<String, Object> data = donorService.getDonorDashboard(userId);
            model.addAttribute("donor", data.get("donor"));
            model.addAttribute("activePage", "campaigns");
            // Ongoing situations must never include resolved, completed, or rejected reports.
            java.util.List<com.ngo.entity.DisasterReport> reports = disasterReportRepository
                    .findAllByOrderByCreatedAtDesc().stream()
                    .filter(report -> report.getStatus() != null
                            && ("active".equalsIgnoreCase(report.getStatus().trim())
                            || "pending".equalsIgnoreCase(report.getStatus().trim())))
                    .collect(Collectors.toList());
            for (com.ngo.entity.DisasterReport r : reports) {
                Double total = donationRepository.getTotalByPurpose(r.getDisasterType());
                r.setAmountRaised(total != null ? total : 0.0);
            }
            model.addAttribute("disasterReports", reports);
            return "kk/campaigns";
        } catch (Exception e) {
            return "redirect:/kk/login";
        }
    }
    @GetMapping("/kk/notifications")
    public String kkNotifications(Model model) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/kk/login";
        Map<String, Object> data = donorService.getDonorDashboard(userId);
        List<Map<String, Object>> notifications = buildDonorNotifications(userId);
        model.addAttribute("donor", data.get("donor")); model.addAttribute("activePage", "notifications");
        model.addAttribute("notifications", notifications); model.addAttribute("headerNotificationCount", notifications.size());
        model.addAttribute("headerNotifications", notifications.size() > 5 ? notifications.subList(0, 5) : notifications);
        return "kk/notifications";
    }
    @GetMapping("/kk/settings") public String kkSettings(Model model) { return renderKkPage("settings", model); }

    @PostMapping("/kk/login")
    public String handleKkLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            HttpServletResponse response, Model model) {
        try {
            LoginResponse lr = authService.donorLogin(new LoginRequest(email, password));
            addJwtCookie(response, lr.getToken());
            return "redirect:/kk/donor";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "kk/login";
        }
    }

    // ─── Dashboards ────────────────────────────────────────────────────────────

    @GetMapping("/nfsa/dashboard")
    public String nfsaDashboard(Model model) {
        try {
            Map<String, Object> data = nfsaService.getDashboardData();
            double td = data.get("totalDonations") == null ? 0 : ((Number) data.get("totalDonations")).doubleValue();
            double ta = data.get("totalAllocated") == null ? 0 : ((Number) data.get("totalAllocated")).doubleValue();
            model.addAttribute("total_donations", td);
            model.addAttribute("total_allocated", ta);
            model.addAttribute("pending_allocations", data.get("pendingAllocations"));
            model.addAttribute("total_beneficiaries", data.get("totalBeneficiaries"));
            model.addAttribute("total_donors", data.get("totalDonors"));
            model.addAttribute("total_admins", data.get("totalAdmins"));
            model.addAttribute("total_users", data.get("totalUsers"));
            model.addAttribute("total_campaigns", data.get("totalCampaigns"));
            model.addAttribute("pending_requests", data.get("pendingRequests"));
            model.addAttribute("active_campaigns", data.get("activeCampaigns"));
            model.addAttribute("unverified_beneficiaries", data.get("unverifiedBeneficiaries"));
            model.addAttribute("recent_donations", data.get("recentDonations"));
            model.addAttribute("percent", td > 0 ? (ta / td * 100) : 0);
            List<com.ngo.entity.DisasterReport> ongoingDisasters = disasterReportRepository
                    .findAllByOrderByCreatedAtDesc().stream()
                    .filter(report -> "active".equalsIgnoreCase(report.getStatus()))
                    .collect(Collectors.toList());
            ongoingDisasters.forEach(report -> {
                Double raised = donationRepository.getTotalByPurpose(report.getDisasterType());
                report.setAmountRaised(raised != null ? raised : 0.0);
            });
            model.addAttribute("ongoingDisasters", ongoingDisasters);
            model.addAttribute("dashboardCampaigns", ongoingDisasters.size() > 3
                    ? ongoingDisasters.subList(0, 3) : ongoingDisasters);

            List<com.ngo.entity.ReliefMaterialRequest> materialRequests = reliefMaterialRequestRepository
                    .findAllByOrderByCreatedAtDesc();
            materialRequests.forEach(request -> beneficiaryRepository.findById(request.getBeneficiaryId())
                    .ifPresent(beneficiary -> request.setBeneficiaryName(beneficiary.getFullName())));
            model.addAttribute("materialRequests", materialRequests);
            model.addAttribute("headerNotificationCount", ongoingDisasters.size() + materialRequests.size());
            model.addAttribute("headerMessageCount", ongoingDisasters.size());
            model.addAttribute("name", getUserName());
        } catch (Exception e) {
            model.addAttribute("name", getUserName());
        }
        return "nfsa/dashboard";
    }

    @GetMapping("/nfsa/beneficiaries")
    public String nfsaBeneficiaryVerification(Model model) {
        try {
            model.addAttribute("beneficiaries", nfsaService.getAllBeneficiaries());
            model.addAttribute("name", getUserName());
        } catch (Exception e) {
            model.addAttribute("name", getUserName());
        }
        return "nfsa/beneficiary_verification";
    }

    @PostMapping("/nfsa/beneficiary-verify")
    public String verifyBeneficiary(
            @RequestParam Long beneficiaryId,
            @RequestParam Boolean verify,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            Long userId = getUserId();
            nfsaService.verifyBeneficiary(beneficiaryId, verify, userId != null ? userId : 0L);
            beneficiaryRepository.findById(beneficiaryId).ifPresent(beneficiary -> emailService.sendStatusEmail(
                    beneficiary.getEmail(), "Account verification update",
                    "Hello " + beneficiary.getFullName() + ",\n\nYour beneficiary account has been "
                            + (verify ? "verified and approved" : "rejected") + "."));
            ra.addFlashAttribute("successMessage",
                    "Beneficiary " + (verify ? "approved" : "rejected") + " successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/nfsa/beneficiaries";
    }

    @GetMapping("/nfsa/donors")
    public String nfsaDonorVerification(Model model) {
        try {
            model.addAttribute("donors", nfsaService.getAllDonors());
            model.addAttribute("name", getUserName());
            
            // Fetch all donations and group by donor ID for the payment history
            java.util.List<Donation> allDonations = donationRepository.findAllByOrderByCreatedAtDesc();
            java.util.Map<Long, java.util.List<Donation>> donorDonations = allDonations.stream()
                .collect(Collectors.groupingBy(Donation::getDonorId));
            model.addAttribute("donorDonations", donorDonations);
        } catch (Exception e) {
            model.addAttribute("name", getUserName());
        }
        return "nfsa/donors";
    }

    @PostMapping("/nfsa/donor-verify")
    public String verifyDonor(
            @RequestParam Long donorId,
            @RequestParam Boolean verify,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            Long userId = getUserId();
            nfsaService.verifyDonor(donorId, verify, userId != null ? userId : 0L);
            ra.addFlashAttribute("successMessage",
                    "Donor " + (verify ? "approved" : "rejected") + " successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/nfsa/donors";
    }

    @GetMapping("/beneficiary/dashboard")
    public String beneficiaryDashboard(Model model) {
        Long userId = getUserId();
        if (userId == null || !"beneficiary".equalsIgnoreCase(getUserRole())) {
            return "redirect:/auth/beneficiary/login";
        }
        if (userId != null) {
            try {
                Map<String, Object> data = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("total_received", data.get("totalReceived"));
                model.addAttribute("total_utilized", data.get("totalUtilized"));
                model.addAttribute("pending_reports", data.get("pendingReports"));
                model.addAttribute("recent_allocations", data.get("recentAllocations"));
                model.addAttribute("beneficiary", data.get("beneficiary"));
                model.addAttribute("total_requests", data.get("totalRequests"));
                model.addAttribute("approved_requests", data.get("approvedRequests"));

                List<DisasterReport> disasterReports = disasterReportRepository
                        .findByReportedByBeneficiaryIdOrderByCreatedAtDesc(userId);
                List<com.ngo.entity.ReliefMaterialRequest> materialRequests = reliefMaterialRequestRepository
                        .findByBeneficiaryIdOrderByCreatedAtDesc(userId);
                long totalRequests = disasterReports.size() + materialRequests.size();
                long approvedRequests = disasterReports.stream().filter(r -> "active".equalsIgnoreCase(r.getStatus())).count()
                        + materialRequests.stream().filter(r -> "approved".equalsIgnoreCase(r.getStatus())
                        || "dispatched".equalsIgnoreCase(r.getStatus()) || "delivered".equalsIgnoreCase(r.getStatus())).count();
                long pendingRequests = disasterReports.stream().filter(r -> "pending".equalsIgnoreCase(r.getStatus())).count()
                        + materialRequests.stream().filter(r -> "pending".equalsIgnoreCase(r.getStatus())).count();
                model.addAttribute("total_requests", totalRequests);
                model.addAttribute("approved_requests", approvedRequests);
                model.addAttribute("pending_reports", pendingRequests);

                List<Map<String, Object>> dashboardRequests = new java.util.ArrayList<>();
                disasterReports.forEach(report -> {
                    Map<String, Object> row = new java.util.HashMap<>(); row.put("id", report.getId()); row.put("kind", "Disaster Report");
                    row.put("purpose", report.getDisasterType()); row.put("date", report.getCreatedAt()); row.put("status", report.getStatus()); row.put("value", report.getRequiredAmount()); dashboardRequests.add(row);
                });
                materialRequests.forEach(request -> {
                    Map<String, Object> row = new java.util.HashMap<>(); row.put("id", request.getId()); row.put("kind", "Material Request");
                    row.put("purpose", request.getMaterialType()); row.put("date", request.getCreatedAt()); row.put("status", request.getStatus()); row.put("value", request.getQuantity()); dashboardRequests.add(row);
                });
                dashboardRequests.sort((a,b) -> ((LocalDateTime)b.get("date")).compareTo((LocalDateTime)a.get("date")));
                List<Map<String, Object>> recentDashboardRequests = dashboardRequests.size() > 5
                        ? new java.util.ArrayList<>(dashboardRequests.subList(0,5)) : dashboardRequests;
                model.addAttribute("dashboardRequests", recentDashboardRequests);

                java.util.Set<String> reportTypes = disasterReports.stream().map(DisasterReport::getDisasterType).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
                List<Donation> receivedTransactions = donationRepository.findAllByOrderByCreatedAtDesc().stream()
                        .filter(d -> d.getPurpose() != null && reportTypes.stream().anyMatch(type -> type.equalsIgnoreCase(d.getPurpose().trim())))
                        .collect(Collectors.toList());
                receivedTransactions.forEach(d -> donorRepository.findById(d.getDonorId()).ifPresent(donor -> d.setDonorName(donor.getFullName())));
                model.addAttribute("total_received", receivedTransactions.stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum());
                model.addAttribute("recentFundTransactions", receivedTransactions.size() > 5 ? receivedTransactions.subList(0,5) : receivedTransactions);

                List<Long> materialRequestIds = materialRequests.stream().map(com.ngo.entity.ReliefMaterialRequest::getId).collect(Collectors.toList());
                List<com.ngo.entity.ReliefMaterialDonation> materialPledges = materialRequestIds.isEmpty() ? new java.util.ArrayList<>()
                        : reliefMaterialDonationRepository.findByRequestIdInOrderByCreatedAtDesc(materialRequestIds);
                materialPledges.forEach(pledge -> reliefMaterialRequestRepository.findById(pledge.getRequestId()).ifPresent(req -> pledge.setMaterialType(req.getMaterialType())));
                Map<String, Integer> materialsSummary = materialPledges.stream().collect(Collectors.groupingBy(
                        p -> p.getMaterialType() == null ? "Material" : p.getMaterialType(), java.util.LinkedHashMap::new,
                        Collectors.summingInt(p -> p.getQuantity() == null ? 0 : p.getQuantity())));
                model.addAttribute("materialsSummary", materialsSummary);
                model.addAttribute("materialItemsTotal", materialPledges.stream().mapToInt(p -> p.getQuantity() == null ? 0 : p.getQuantity()).sum());

                List<DisasterReport> dashboardCampaigns = disasterReportRepository.findAllByOrderByCreatedAtDesc();
                dashboardCampaigns.forEach(report -> {
                    Double raised = donationRepository.getTotalByPurpose(report.getDisasterType());
                    report.setAmountRaised(raised != null ? raised : 0.0);
                });
                List<DisasterReport> activeDashboardCampaigns = dashboardCampaigns.stream()
                        .filter(report -> report.getStatus() != null && "active".equalsIgnoreCase(report.getStatus().trim()))
                        .limit(3).collect(Collectors.toList());
                List<DisasterReport> completedDashboardCampaigns = dashboardCampaigns.stream()
                        .filter(report -> report.getStatus() != null && ("completed".equalsIgnoreCase(report.getStatus().trim())
                                || "resolved".equalsIgnoreCase(report.getStatus().trim())))
                        .limit(3).collect(Collectors.toList());
                model.addAttribute("activeDashboardCampaigns", activeDashboardCampaigns);
                model.addAttribute("completedDashboardCampaigns", completedDashboardCampaigns);
                List<DisasterReport> campaignMessages = dashboardCampaigns.stream()
                        .filter(report -> report.getStatus() != null && !"rejected".equalsIgnoreCase(report.getStatus().trim()))
                        .collect(Collectors.toList());
                model.addAttribute("messageCount", campaignMessages.size());
                model.addAttribute("headerMessages", campaignMessages.size() > 5 ? campaignMessages.subList(0, 5) : campaignMessages);
                List<Map<String, Object>> notifications = buildBeneficiaryNotifications(userId);
                model.addAttribute("notificationCount", notifications.size());
                model.addAttribute("headerNotifications", notifications.size() > 5 ? notifications.subList(0, 5) : notifications);
            } catch (Exception exception) {
                return "redirect:/auth/beneficiary/login";
            }
        }
        return "beneficiary/dashboard";
    }

    // ─── Password / Auth Misc ──────────────────────────────────────────────────

    @GetMapping({"/forgot-password", "/forgot_password", "/auth/forgot_password"})
    public String forgotPassword(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email != null && passwordResetService.hasActiveOtp(email)) {
            model.addAttribute("otpEmail", email);
            model.addAttribute("enteredEmail", email);
        }
        return "forgot_password";
    }

    @PostMapping({"/forgot-password", "/forgot_password"})
    public String submitForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            var resetRequest = passwordResetService.requestPasswordReset(email.trim());
            emailService.sendPasswordResetOtpEmail(resetRequest.getEmail(),
                    passwordResetService.getAccountName(resetRequest), resetRequest.getOtpCode());
            redirectAttributes.addFlashAttribute("success", "A six-digit OTP has been sent to your email.");
            redirectAttributes.addAttribute("email", resetRequest.getEmail());
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            redirectAttributes.addFlashAttribute("enteredEmail", email);
        }
        return "redirect:/forgot-password";
    }

    @PostMapping("/forgot-password/verify-otp")
    public String verifyPasswordResetOtp(@RequestParam String email, @RequestParam String otp,
                                         RedirectAttributes redirectAttributes) {
        try {
            var resetRequest = passwordResetService.verifyOtp(email.trim(), otp.trim());
            return "redirect:/reset-password/" + resetRequest.getToken();
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            redirectAttributes.addAttribute("email", email.trim());
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/forgot-email")
    public String forgotEmail() { return "forgot_email"; }

    @PostMapping("/forgot-email")
    public String findForgottenEmail(@RequestParam String phone, Model model) {
        String cleanPhone = phone == null ? "" : phone.trim();
        String email = nfsaRepository.findFirstByPhone(cleanPhone).map(NFSA::getEmail).orElse(null);
        if (email == null) {
            email = donorRepository.findFirstByPhone(cleanPhone).map(Donor::getEmail).orElse(null);
        }
        if (email == null) {
            email = beneficiaryRepository.findFirstByPhone(cleanPhone).map(Beneficiary::getEmail).orElse(null);
        }

        if (email == null) {
            model.addAttribute("error", "No registered account matches that phone number.");
        } else {
            model.addAttribute("maskedEmail", maskEmail(email));
        }
        model.addAttribute("enteredPhone", cleanPhone);
        return "forgot_email";
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(at, 0));
        int visible = Math.min(2, at);
        return email.substring(0, visible) + "*".repeat(Math.max(3, at - visible)) + email.substring(at);
    }

    @GetMapping("/auth/reset_password")
    public String resetPassword() { return "redirect:/forgot-password"; }

    @GetMapping("/reset-password/{token}")
    public String showResetPassword(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        if (!passwordResetService.isValidResetToken(token)) {
            model.addAttribute("error", "This password reset link is invalid or has expired.");
            model.addAttribute("invalidToken", true);
        }
        return "reset_password";
    }

    @PostMapping("/complete-password-reset")
    public String completePasswordReset(@RequestParam String token,
                                        @RequestParam String password,
                                        @RequestParam("confirm_password") String confirmPassword,
                                        RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/reset-password/" + token;
        }
        try {
            passwordResetService.completePasswordReset(token, password);
            redirectAttributes.addFlashAttribute("success", "Password reset successful. You can now log in.");
            return "redirect:/forgot-password";
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/reset-password/" + token;
        }
    }


    // ─── Features ──────────────────────────────────────────────────────────────

    @GetMapping("/features/jwt")
    public String featureJwt() { return "features/jwt_security"; }

    @GetMapping("/features/tracking")
    public String featureTracking() { return "features/fund_tracking"; }

    @GetMapping("/features/verification")
    public String featureVerification() { return "features/verification"; }

    @GetMapping("/features/transparency")
    public String featureTransparency() { return "features/transparency"; }

    // ─── NFSA Sub-Pages ────────────────────────────────────────────────────────

    @GetMapping("/nfsa/users")
    public String nfsaUsers(Model model) {
        try {
            List<Donor> donors = donorRepository.findAll();
            List<Beneficiary> beneficiaries = beneficiaryRepository.findAll();
            List<NFSA> nfsaAdmins = nfsaRepository.findAll();

            model.addAttribute("donors", donors);
            model.addAttribute("beneficiaries", beneficiaries);
            model.addAttribute("nfsaAdmins", nfsaAdmins);

            long totalDonors = donors.size();
            long totalBeneficiaries = beneficiaries.size();
            long totalAdmins = nfsaAdmins.size();
            model.addAttribute("totalUsers", totalDonors + totalBeneficiaries + totalAdmins);
            model.addAttribute("totalDonors", totalDonors);
            model.addAttribute("totalBeneficiaries", totalBeneficiaries);
            model.addAttribute("totalAdmins", totalAdmins);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "nfsa/users";
    }

    @PostMapping("/nfsa/users/update")
    public String updatePlatformUser(@RequestParam String role, @RequestParam Long userId,
                                     @RequestParam String fullName, @RequestParam String email,
                                     @RequestParam String phone, @RequestParam(defaultValue = "") String address,
                                     @RequestParam(defaultValue = "") String needCategory,
                                     @RequestParam(defaultValue = "1") Integer familyMembers,
                                     @RequestParam(defaultValue = "0") Double monthlyIncome,
                                     @RequestParam(defaultValue = "") String organizationName,
                                     @RequestParam(defaultValue = "") String designation,
                                     @RequestParam(defaultValue = "") String employeeId,
                                     @RequestParam(defaultValue = "true") Boolean verified,
                                     @RequestParam(defaultValue = "") String newPassword,
                                     RedirectAttributes ra) {
        try {
            String recipient;
            if ("kk".equalsIgnoreCase(role)) {
                Donor user = donorRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("KK account not found."));
                user.setFullName(fullName.trim()); user.setEmail(email.trim()); user.setPhone(phone.trim()); user.setAddress(address.trim()); user.setIsVerified(verified);
                if (!newPassword.isBlank()) user.setPasswordHash(passwordEncoder.encode(newPassword)); donorRepository.save(user); recipient = user.getEmail();
            } else if ("beneficiary".equalsIgnoreCase(role)) {
                Beneficiary user = beneficiaryRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Beneficiary not found."));
                user.setFullName(fullName.trim()); user.setEmail(email.trim()); user.setPhone(phone.trim()); user.setAddress(address.trim()); user.setNeedCategory(needCategory.trim());
                user.setFamilyMembers(familyMembers); user.setMonthlyIncome(monthlyIncome); user.setIsVerified(verified);
                if (!newPassword.isBlank()) user.setPasswordHash(passwordEncoder.encode(newPassword)); beneficiaryRepository.save(user); recipient = user.getEmail();
            } else if ("admin".equalsIgnoreCase(role)) {
                NFSA user = nfsaRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("NFSA administrator not found."));
                user.setFullName(fullName.trim()); user.setEmail(email.trim()); user.setPhone(phone.trim()); user.setOrganizationName(organizationName.trim());
                user.setDesignation(designation.trim()); user.setEmployeeId(employeeId.trim()); user.setIsVerified(verified);
                if (!newPassword.isBlank()) user.setPasswordHash(passwordEncoder.encode(newPassword)); nfsaRepository.save(user); recipient = user.getEmail();
            } else throw new IllegalArgumentException("Invalid account role.");
            emailService.sendStatusEmail(recipient, "Account details updated", "Your registered account details"
                    + (!newPassword.isBlank() ? " and password were" : " were") + " updated by the platform administrator.");
            ra.addFlashAttribute("successMessage", "Account updated successfully.");
        } catch (Exception exception) { ra.addFlashAttribute("errorMessage", "Unable to update account: " + exception.getMessage()); }
        return "redirect:/nfsa/users";
    }

    @PostMapping("/nfsa/users/delete")
    public String deletePlatformUser(@RequestParam String role, @RequestParam Long userId,
                                     HttpServletRequest request, HttpServletResponse response,
                                     RedirectAttributes ra) {
        try {
            Long adminId = getUserId();
            if (adminId == null || !"nfsa".equalsIgnoreCase(getUserRole())) throw new IllegalStateException("Not authorized.");
            if ("kk".equalsIgnoreCase(role)) nfsaService.removeDonor(userId, adminId);
            else if ("beneficiary".equalsIgnoreCase(role)) nfsaService.removeBeneficiary(userId, adminId);
            else if ("admin".equalsIgnoreCase(role)) {
                boolean deletingCurrentAccount = adminId.equals(userId);
                nfsaService.removeNfsaAdmin(userId, adminId, deletingCurrentAccount);
                if (deletingCurrentAccount) {
                    clearJwtCookie(response);
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false) != null) request.getSession(false).invalidate();
                    return "redirect:/auth/nfsa/signup?accountDeleted=true";
                }
            } else throw new IllegalArgumentException("Invalid account role.");
            ra.addFlashAttribute("successMessage", "Account deleted successfully.");
        } catch (Exception exception) { ra.addFlashAttribute("errorMessage", "Unable to delete account: " + exception.getMessage()); }
        return "redirect:/nfsa/users";
    }

    @PostMapping("/nfsa/users/clear-previous-data")
    public String clearPreviousPlatformData(RedirectAttributes ra) {
        try {
            Long adminId = getUserId();
            if (adminId == null || !"nfsa".equalsIgnoreCase(getUserRole())) {
                throw new IllegalStateException("Not authorized.");
            }
            nfsaService.clearPreviousPlatformData(adminId);
            ra.addFlashAttribute("successMessage", "Previous platform data cleared. This administrator account was retained as the only fresh user.");
        } catch (Exception exception) {
            ra.addFlashAttribute("errorMessage", "Unable to clear previous data: " + exception.getMessage());
        }
        return "redirect:/nfsa/users";
    }

    @GetMapping("/nfsa/ngos")
    public String nfsaNgos(Model model) {
        List<Map<String, Object>> accounts = new java.util.ArrayList<>();
        nfsaRepository.findAll().stream().filter(user -> Boolean.TRUE.equals(user.getIsVerified())).forEach(user -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", user.getId()); row.put("name", user.getFullName()); row.put("email", user.getEmail());
            row.put("phone", user.getPhone()); row.put("role", "NFSA / Organization"); row.put("organization", user.getOrganizationName());
            row.put("detail", user.getDesignation()); row.put("createdAt", user.getCreatedAt()); accounts.add(row);
        });
        donorRepository.findAll().stream().filter(user -> Boolean.TRUE.equals(user.getIsVerified())).forEach(user -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", user.getId()); row.put("name", user.getFullName()); row.put("email", user.getEmail());
            row.put("phone", user.getPhone()); row.put("role", "Verified Donor"); row.put("organization", "Independent Donor");
            row.put("detail", user.getAddress()); row.put("createdAt", user.getCreatedAt()); accounts.add(row);
        });
        beneficiaryRepository.findAll().stream().filter(user -> Boolean.TRUE.equals(user.getIsVerified())).forEach(user -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", user.getId()); row.put("name", user.getFullName()); row.put("email", user.getEmail());
            row.put("phone", user.getPhone()); row.put("role", "Verified Beneficiary"); row.put("organization", "Beneficiary Account");
            row.put("detail", user.getNeedCategory()); row.put("createdAt", user.getCreatedAt()); accounts.add(row);
        });
        accounts.sort((a,b) -> ((LocalDateTime)b.get("createdAt")).compareTo((LocalDateTime)a.get("createdAt")));
        model.addAttribute("verifiedAccounts", accounts);
        model.addAttribute("verifiedTotal", accounts.size());
        model.addAttribute("verifiedOrganizations", accounts.stream().filter(a -> "NFSA / Organization".equals(a.get("role"))).count());
        model.addAttribute("verifiedDonors", accounts.stream().filter(a -> "Verified Donor".equals(a.get("role"))).count());
        model.addAttribute("verifiedBeneficiaries", accounts.stream().filter(a -> "Verified Beneficiary".equals(a.get("role"))).count());
        model.addAttribute("activePage", "ngos");
        return "nfsa/ngos";
    }

    @GetMapping("/nfsa/campaigns")
    public String nfsaCampaigns(Model model) {
        List<DisasterReport> ongoingCampaignRows = disasterReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(report -> report.getStatus() != null
                        && ("active".equalsIgnoreCase(report.getStatus().trim())
                        || "pending".equalsIgnoreCase(report.getStatus().trim())))
                .collect(Collectors.toList());

        java.util.LinkedHashMap<String, DisasterReport> newestCampaignByType = new java.util.LinkedHashMap<>();
        ongoingCampaignRows.forEach(report -> newestCampaignByType.putIfAbsent(
                report.getDisasterType() == null ? "unknown-" + report.getId() : report.getDisasterType().trim().toLowerCase(), report));
        List<DisasterReport> campaigns = new java.util.ArrayList<>(newestCampaignByType.values());

        campaigns.forEach(report -> {
            report.setStatus(report.getStatus().trim().toLowerCase());
            Double raised = donationRepository.getTotalByPurposeSince(report.getDisasterType(), report.getCreatedAt());
            report.setAmountRaised(raised != null ? raised : 0.0);
            if (report.getReportedByBeneficiaryId() != null) {
                beneficiaryRepository.findById(report.getReportedByBeneficiaryId())
                        .ifPresent(beneficiary -> report.setBeneficiaryName(beneficiary.getFullName()));
            }
        });

        List<DisasterReport> completedCampaigns = disasterReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(report -> report.getStatus() != null
                        && ("completed".equalsIgnoreCase(report.getStatus().trim())
                        || "resolved".equalsIgnoreCase(report.getStatus().trim())))
                .collect(Collectors.toList());
        completedCampaigns.forEach(report -> {
            Double raised = donationRepository.getTotalByPurposeSince(report.getDisasterType(), report.getCreatedAt());
            report.setAmountRaised(raised != null ? raised : 0.0);
            if (report.getReportedByBeneficiaryId() != null) {
                beneficiaryRepository.findById(report.getReportedByBeneficiaryId())
                        .ifPresent(beneficiary -> report.setBeneficiaryName(beneficiary.getFullName()));
            }
        });

        int affectedFamilies = campaigns.stream()
                .mapToInt(report -> report.getFamiliesAffected() != null ? report.getFamiliesAffected() : 0)
                .sum();
        double totalRequired = campaigns.stream()
                .mapToDouble(report -> report.getRequiredAmount() != null ? report.getRequiredAmount() : 0.0)
                .sum();
        double totalRaised = campaigns.stream()
                .mapToDouble(report -> report.getAmountRaised() != null ? report.getAmountRaised() : 0.0)
                .sum();

        long activeCampaigns = campaigns.stream()
                .filter(report -> "active".equalsIgnoreCase(report.getStatus().trim())).count();
        long pendingCampaigns = campaigns.stream()
                .filter(report -> "pending".equalsIgnoreCase(report.getStatus().trim())).count();

        model.addAttribute("campaigns", campaigns);
        model.addAttribute("completedCampaigns", completedCampaigns);
        model.addAttribute("activeCampaigns", activeCampaigns);
        model.addAttribute("pendingCampaigns", pendingCampaigns);
        model.addAttribute("affectedFamilies", affectedFamilies);
        model.addAttribute("totalRequired", totalRequired);
        model.addAttribute("totalRaised", totalRaised);
        model.addAttribute("activePage", "campaigns");
        return "nfsa/campaigns";
    }

    @PostMapping("/kk/donate/material")
    public String donateReliefMaterials(@RequestParam Long materialRequestId,
                                        @RequestParam Integer materialQuantity,
                                        RedirectAttributes ra) {
        Long donorId = getUserId();
        if (donorId == null) return "redirect:/kk/login";
        try {
            com.ngo.entity.ReliefMaterialRequest request = reliefMaterialRequestRepository
                    .findById(materialRequestId)
                    .orElseThrow(() -> new IllegalArgumentException("Material request not found."));
            if (!"approved".equalsIgnoreCase(request.getStatus())) {
                throw new IllegalArgumentException("This material request is no longer accepting donations.");
            }
            if (materialQuantity == null || materialQuantity < 1) {
                throw new IllegalArgumentException("Please enter a valid material quantity.");
            }
            long donated = reliefMaterialDonationRepository.getDonatedQuantity(materialRequestId);
            long remaining = Math.max(0, request.getQuantity() - donated);
            if (materialQuantity > remaining) {
                throw new IllegalArgumentException("Only " + remaining + " item(s) are still required.");
            }
            com.ngo.entity.ReliefMaterialDonation donation = new com.ngo.entity.ReliefMaterialDonation();
            donation.setRequestId(materialRequestId);
            donation.setDonorId(donorId);
            donation.setQuantity(materialQuantity);
            donation.setStatus("pledged");
            reliefMaterialDonationRepository.save(donation);
            ra.addFlashAttribute("successMessage", "Thank you! You pledged " + materialQuantity
                    + " item(s) for material request #" + materialRequestId + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/kk/donate";
    }

    @GetMapping("/nfsa/donations")
    public String nfsaDonations(Model model) {
        List<Donation> donations = donationRepository.findAllByOrderByCreatedAtDesc();
        donations.forEach(donation -> {
            donorRepository.findById(donation.getDonorId())
                    .ifPresent(donor -> donation.setDonorName(donor.getFullName()));
            double donationAllocated = allocationRepository.findByDonationId(donation.getId()).stream()
                    .mapToDouble(allocation -> allocation.getAmount() != null ? allocation.getAmount() : 0.0)
                    .sum();
            donation.setAllocatedAmount(donationAllocated);
        });

        List<com.ngo.entity.ReliefMaterialDonation> materialDonations =
                reliefMaterialDonationRepository.findAll();
        materialDonations.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        materialDonations.forEach(donation -> {
            donorRepository.findById(donation.getDonorId())
                    .ifPresent(donor -> donation.setDonorName(donor.getFullName()));
            reliefMaterialRequestRepository.findById(donation.getRequestId()).ifPresent(request -> {
                donation.setMaterialType(request.getMaterialType());
                beneficiaryRepository.findById(request.getBeneficiaryId())
                        .ifPresent(beneficiary -> donation.setBeneficiaryName(beneficiary.getFullName()));
            });
        });

        double totalAmount = donations.stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum();
        double allocatedAmount = allocationRepository.findAll().stream()
                .mapToDouble(allocation -> allocation.getAmount() != null ? allocation.getAmount() : 0.0)
                .sum();
        int materialItems = materialDonations.stream().mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum();
        long uniqueDonors = java.util.stream.Stream.concat(
                donations.stream().map(Donation::getDonorId), materialDonations.stream().map(com.ngo.entity.ReliefMaterialDonation::getDonorId))
                .distinct().count();

        model.addAttribute("donations", donations);
        model.addAttribute("materialDonations", materialDonations);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("allocatedAmount", allocatedAmount);
        model.addAttribute("materialItems", materialItems);
        model.addAttribute("uniqueDonors", uniqueDonors);
        model.addAttribute("activePage", "donations");
        return "nfsa/donations";
    }

    @GetMapping("/nfsa/relief-requests")
    public String nfsaReliefRequests(Model model) {
        List<com.ngo.entity.ReliefMaterialRequest> requests =
                reliefMaterialRequestRepository.findAllByOrderByCreatedAtDesc();
        requests.forEach(request -> {
            request.setStatus(request.getStatus() == null ? "pending" : request.getStatus().trim().toLowerCase());
            beneficiaryRepository.findById(request.getBeneficiaryId())
                    .ifPresent(beneficiary -> request.setBeneficiaryName(beneficiary.getFullName()));
            request.setDonatedQuantity(reliefMaterialDonationRepository.getDonatedQuantity(request.getId()));
        });
        model.addAttribute("materialRequests", requests);
        model.addAttribute("totalRequests", requests.size());
        model.addAttribute("pendingRequests", requests.stream().filter(r -> "pending".equals(r.getStatus())).count());
        model.addAttribute("approvedRequests", requests.stream().filter(r -> "approved".equals(r.getStatus())).count());
        model.addAttribute("dispatchedRequests", requests.stream().filter(r -> "dispatched".equals(r.getStatus())).count());
        model.addAttribute("deliveredRequests", requests.stream().filter(r -> "delivered".equals(r.getStatus())).count());
        model.addAttribute("activePage", "relief_requests");
        return "nfsa/relief_requests";
    }

    @PostMapping("/nfsa/relief-requests/update-status")
    public String updateReliefMaterialStatus(@RequestParam Long requestId,
                                             @RequestParam String status,
                                             RedirectAttributes ra) {
        if (getUserId() == null) return "redirect:/auth/nfsa/login";
        try {
            status = status == null ? "" : status.trim().toLowerCase();
            final String emailStatus = status;
            if (!List.of("approved", "rejected", "dispatched", "delivered").contains(status)) {
                throw new IllegalArgumentException("Invalid material request status.");
            }
            com.ngo.entity.ReliefMaterialRequest request = reliefMaterialRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("Material request not found."));
            request.setStatus(status);
            reliefMaterialRequestRepository.save(request);
            beneficiaryRepository.findById(request.getBeneficiaryId()).ifPresent(beneficiary ->
                    emailService.sendStatusEmail(beneficiary.getEmail(), "Material request status updated",
                            "Hello " + beneficiary.getFullName() + ",\n\nYour material request #" + requestId
                                    + " for " + request.getMaterialType() + " is now " + emailStatus + "."));
            ra.addFlashAttribute("successMessage", "Material request #" + requestId + " marked as " + status + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nfsa/relief-requests";
    }

    @GetMapping("/nfsa/fund-management")
    public String nfsaFundManagement(Model model) {
        List<Donation> donations = donationRepository.findAllByOrderByCreatedAtDesc();
        donations.forEach(donation -> donorRepository.findById(donation.getDonorId())
                .ifPresent(donor -> donation.setDonorName(donor.getFullName())));
        List<Allocation> allocations = allocationRepository.findAllByOrderByCreatedAtDesc();
        allocations.forEach(allocation -> {
            beneficiaryRepository.findById(allocation.getBeneficiaryId())
                    .ifPresent(beneficiary -> allocation.setBeneficiaryName(beneficiary.getFullName()));
            donationRepository.findById(allocation.getDonationId())
                    .ifPresent(donation -> allocation.setDonationPurpose(donation.getPurpose()));
        });

        double totalFunds = donations.stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum();
        double totalAllocated = allocations.stream().mapToDouble(a -> a.getAmount() != null ? a.getAmount() : 0.0).sum();
        double availableFunds = Math.max(0.0, totalFunds - totalAllocated);
        Map<String, Double> fundsByCause = donations.stream().collect(Collectors.groupingBy(
                d -> d.getPurpose() == null || d.getPurpose().isBlank() ? "General Fund" : d.getPurpose(),
                java.util.LinkedHashMap::new,
                Collectors.summingDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0)));

        model.addAttribute("fundTransactions", donations);
        model.addAttribute("allocations", allocations);
        model.addAttribute("totalFunds", totalFunds);
        model.addAttribute("totalAllocated", totalAllocated);
        model.addAttribute("availableFunds", availableFunds);
        model.addAttribute("transactionCount", donations.size());
        model.addAttribute("fundsByCause", fundsByCause);
        model.addAttribute("activePage", "fund_management");
        return "nfsa/fund_management";
    }

    @GetMapping("/nfsa/disaster-management")
    public String nfsaDisasterManagement(Model model) {
        try {
            List<DisasterReport> reports = disasterReportRepository.findAllByOrderByCreatedAtDesc();
            reports.forEach(report -> {
                report.setStatus(report.getStatus() == null ? "pending" : report.getStatus().trim().toLowerCase());
                if (report.getReportedByBeneficiaryId() != null) {
                    beneficiaryRepository.findById(report.getReportedByBeneficiaryId())
                            .ifPresent(b -> report.setBeneficiaryName(b.getFullName()));
                }
            });
            long totalReports = reports.size();
            long pendingReports = reports.stream().filter(r -> "pending".equals(r.getStatus())).count();
            long activeReports = reports.stream().filter(r -> "active".equals(r.getStatus())).count();
            long resolvedReports = reports.stream().filter(r -> "resolved".equals(r.getStatus())).count();
            long rejectedReports = reports.stream().filter(r -> "rejected".equals(r.getStatus())).count();

            model.addAttribute("disasterReports", reports);
            model.addAttribute("totalReports", totalReports);
            model.addAttribute("pendingReports", pendingReports);
            model.addAttribute("activeReports", activeReports);
            model.addAttribute("resolvedReports", resolvedReports);
            model.addAttribute("rejectedReports", rejectedReports);
            model.addAttribute("activePage", "disaster_management");
        } catch (Exception ignored) {
        }
        return "nfsa/disaster_management";
    }

    @PostMapping("/nfsa/disaster-management/update-status")
    public String updateDisasterReportStatus(@RequestParam Long reportId,
                                             @RequestParam String status,
                                             @RequestParam(defaultValue = "/nfsa/disaster-management") String redirectUrl,
                                             RedirectAttributes ra) {
        String safeRedirect = "/nfsa/campaigns".equals(redirectUrl)
                ? "/nfsa/campaigns" : "/nfsa/disaster-management";
        Long userId = getUserId();
        if (userId == null) {
            ra.addFlashAttribute("errorMessage", "Unable to update report status: not authenticated.");
            return "redirect:" + safeRedirect;
        }
        try {
            com.ngo.entity.DisasterReport report = disasterReportRepository.findById(reportId)
                    .orElseThrow(() -> new IllegalArgumentException("Disaster report not found."));
            nfsaService.updateDisasterReportStatus(reportId, status, userId);
            if ("active".equalsIgnoreCase(status)) {
                List<String> recipients = new java.util.ArrayList<>();
                beneficiaryRepository.findAll().forEach(user -> recipients.add(user.getEmail()));
                donorRepository.findAll().forEach(user -> recipients.add(user.getEmail()));
                nfsaRepository.findAll().forEach(user -> recipients.add(user.getEmail()));
                emailService.sendEmergencyAlert(recipients, report);
            }
            if (report.getReportedByBeneficiaryId() != null) {
                beneficiaryRepository.findById(report.getReportedByBeneficiaryId()).ifPresent(beneficiary ->
                        emailService.sendStatusEmail(beneficiary.getEmail(), "Disaster report status updated",
                                "Hello " + beneficiary.getFullName() + ",\n\nYour " + report.getDisasterType()
                                        + " disaster report #" + reportId + " is now " + status + "."));
            }
            if ("active".equalsIgnoreCase(status)) {
                ra.addFlashAttribute("successMessage", "Disaster report approved successfully.");
            } else if ("rejected".equalsIgnoreCase(status)) {
                ra.addFlashAttribute("successMessage", "Disaster report disapproved successfully.");
            } else if ("resolved".equals(status)) {
                ra.addFlashAttribute("successMessage", "Disaster report marked as resolved.");
            } else {
                ra.addFlashAttribute("successMessage", "Disaster report status updated.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update report status: " + e.getMessage());
        }
        return "redirect:" + safeRedirect;
    }

    @GetMapping("/nfsa/reports")
    public String nfsaReports() { return "nfsa/reports"; }

    @GetMapping("/nfsa/notifications")
    public String nfsaNotifications(Model model) {
        model.addAttribute("notifications", buildNfsaNotifications());
        model.addAttribute("activePage", "notifications");
        return "nfsa/notifications";
    }

    @GetMapping("/nfsa/messages")
    public String nfsaMessages(Model model) {
        model.addAttribute("messages", buildNfsaNotifications());
        return "nfsa/messages";
    }

    @GetMapping("/nfsa/email-alerts")
    public String nfsaEmailAlerts(Model model) {
        List<DisasterReport> ongoing = disasterReportRepository.findByStatusOrderByCreatedAtDesc("active");
        long recipients = java.util.stream.Stream.of(
                        beneficiaryRepository.findAll().stream().map(com.ngo.entity.Beneficiary::getEmail),
                        donorRepository.findAll().stream().map(com.ngo.entity.Donor::getEmail),
                        nfsaRepository.findAll().stream().map(com.ngo.entity.NFSA::getEmail))
                .flatMap(java.util.function.Function.identity()).filter(java.util.Objects::nonNull)
                .map(String::trim).filter(email -> !email.isBlank()).distinct().count();
        model.addAttribute("ongoingDisasters", ongoing);
        model.addAttribute("ongoingCount", ongoing.size());
        model.addAttribute("recipientCount", recipients);
        model.addAttribute("gmailStatus", emailService.getConnectionStatus());
        model.addAttribute("activePage", "email_alerts");
        return "nfsa/email";
    }

    @PostMapping("/nfsa/email-alerts/send")
    public String sendDisasterEmailAlert(@RequestParam Long reportId, RedirectAttributes ra) {
        try {
            DisasterReport report = disasterReportRepository.findById(reportId)
                    .orElseThrow(() -> new IllegalArgumentException("Disaster report not found."));
            if (!"active".equalsIgnoreCase(report.getStatus())) throw new IllegalArgumentException("Only ongoing disasters can be emailed.");
            java.util.Set<String> recipients = new java.util.LinkedHashSet<>();
            beneficiaryRepository.findAll().forEach(user -> recipients.add(user.getEmail()));
            donorRepository.findAll().forEach(user -> recipients.add(user.getEmail()));
            nfsaRepository.findAll().forEach(user -> recipients.add(user.getEmail()));
            recipients.removeIf(email -> email == null || email.isBlank());
            emailService.sendEmergencyAlert(recipients, report);
            ra.addFlashAttribute("successMessage", "Emergency alert queued for " + recipients.size() + " registered users.");
        } catch (Exception exception) {
            ra.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/nfsa/email-alerts";
    }

    @GetMapping("/nfsa/support-tickets")
    public String nfsaSupportTickets() { return "nfsa/support_tickets"; }

    @GetMapping("/nfsa/settings")
    public String nfsaSettings(Model model) {
        model.addAttribute("gmailStatus", emailService.getConnectionStatus());
        model.addAttribute("gmailAddress", emailService.getConfiguredAddress());
        return "nfsa/settings";
    }

    @PostMapping("/nfsa/settings/gmail")
    public String configureGmail(@RequestParam String gmailUsername,
                                 @RequestParam(required = false) String gmailAppPassword,
                                 @RequestParam(defaultValue = "false") boolean mailEnabled,
                                 RedirectAttributes redirectAttributes) {
        var status = emailService.configureAndConnect(gmailUsername, gmailAppPassword, mailEnabled);
        if (status.connected()) {
            redirectAttributes.addFlashAttribute("successMessage", "Gmail connected successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", status.detail());
        }
        return "redirect:/nfsa/settings";
    }

    @GetMapping("/nfsa/audit-logs")
    public String nfsaAuditLogs() { return "nfsa/audit_logs"; }
    // ─── Donor Sub-Pages ───────────────────────────────────────────────────────


    // ─── Beneficiary Sub-Pages ─────────────────────────────────────────────────

    @GetMapping("/beneficiary_allocated_funds")
    public String beneficiaryAllocatedFunds(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                model.addAttribute("funds", beneficiaryService.getAllocatedFunds(userId));
                Map<String, Object> dashData = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dashData.get("beneficiary"));
            } catch (Exception ignored) {}
        }
        return "beneficiary/allocated_funds";
    }


    @GetMapping("/beneficiary_assistance_history")
    public String beneficiaryAssistanceHistory(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> data = beneficiaryService.getAssistanceHistory(userId);
                model.addAttribute("allocations", data.get("allocations"));
                model.addAttribute("reports", data.get("reports"));
                model.addAttribute("total_received", data.get("totalReceived"));
                model.addAttribute("total_utilized", data.get("totalUtilized"));
                Map<String, Object> dashData = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dashData.get("beneficiary"));
            } catch (Exception ignored) {}
        }
        return "beneficiary/assistance_history";
    }

    @GetMapping("/beneficiary/profile")
    public String beneficiaryProfile(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> data = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", data.get("beneficiary"));
            } catch (Exception ignored) {}
        }
        return "beneficiary/profile";
    }

    @GetMapping("/beneficiary/my-requests")
    public String beneficiaryMyRequests(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> data = beneficiaryService.getAssistanceHistory(userId);
                model.addAttribute("allocations", data.get("allocations"));
                model.addAttribute("total_received", data.get("totalReceived"));
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
                List<DisasterReport> disasterReports = disasterReportRepository
                        .findByReportedByBeneficiaryIdOrderByCreatedAtDesc(userId);
                List<com.ngo.entity.ReliefMaterialRequest> materialRequests = reliefMaterialRequestRepository
                        .findByBeneficiaryIdOrderByCreatedAtDesc(userId);
                materialRequests.forEach(request -> request.setDonatedQuantity(
                        reliefMaterialDonationRepository.getDonatedQuantity(request.getId())));
                model.addAttribute("disasterReports", disasterReports);
                model.addAttribute("materialRequests", materialRequests);
                model.addAttribute("totalRequestCount", disasterReports.size() + materialRequests.size());
                model.addAttribute("disasterReportCount", disasterReports.size());
                model.addAttribute("materialRequestCount", materialRequests.size());
                java.util.Set<String> reportTypes = disasterReports.stream().map(DisasterReport::getDisasterType)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
                List<Donation> receivedFundTransactions = donationRepository.findAllByOrderByCreatedAtDesc().stream()
                        .filter(donation -> donation.getPurpose() != null && reportTypes.stream()
                                .anyMatch(type -> type.equalsIgnoreCase(donation.getPurpose().trim())))
                        .collect(Collectors.toList());
                receivedFundTransactions.forEach(donation -> donorRepository.findById(donation.getDonorId())
                        .ifPresent(donor -> donation.setDonorName(donor.getFullName())));
                double transactionFundsReceived = receivedFundTransactions.stream()
                        .mapToDouble(donation -> donation.getAmount() != null ? donation.getAmount() : 0.0).sum();
                model.addAttribute("receivedFundTransactions", receivedFundTransactions);
                model.addAttribute("total_received", transactionFundsReceived);
            } catch (Exception ignored) {}
        }
        return "beneficiary/my_requests";
    }

    @GetMapping("/beneficiary/funds-received")
    public String beneficiaryFundsReceived(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                List<Map<String, Object>> funds = beneficiaryService.getAllocatedFunds(userId);
                funds.forEach(item -> {
                    Object donorId = item.get("donorId");
                    if (donorId instanceof Long) donorRepository.findById((Long) donorId)
                            .ifPresent(donor -> item.put("donorName", donor.getFullName()));
                });
                model.addAttribute("funds", funds);
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
                List<Map<String, Object>> notifications = buildBeneficiaryNotifications(userId);
                model.addAttribute("notifications", notifications);
                model.addAttribute("notificationCount", notifications.size());
                model.addAttribute("total_received", dash.get("totalReceived"));

                List<Long> requestIds = reliefMaterialRequestRepository
                        .findByBeneficiaryIdOrderByCreatedAtDesc(userId).stream()
                        .map(com.ngo.entity.ReliefMaterialRequest::getId).collect(Collectors.toList());
                List<com.ngo.entity.ReliefMaterialDonation> materialPledges = requestIds.isEmpty()
                        ? new java.util.ArrayList<>()
                        : reliefMaterialDonationRepository.findByRequestIdInOrderByCreatedAtDesc(requestIds);
                materialPledges.forEach(pledge -> {
                    donorRepository.findById(pledge.getDonorId())
                            .ifPresent(donor -> pledge.setDonorName(donor.getFullName()));
                    reliefMaterialRequestRepository.findById(pledge.getRequestId())
                            .ifPresent(request -> pledge.setMaterialType(request.getMaterialType()));
                });
                model.addAttribute("materialPledges", materialPledges);
                model.addAttribute("materialItemsReceived", materialPledges.stream()
                        .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum());

                java.util.Set<String> beneficiaryCampaignTypes = disasterReportRepository
                        .findByReportedByBeneficiaryIdOrderByCreatedAtDesc(userId).stream()
                        .map(DisasterReport::getDisasterType).filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
                List<Donation> beneficiaryFundTransactions = donationRepository.findAllByOrderByCreatedAtDesc().stream()
                        .filter(donation -> donation.getPurpose() != null && beneficiaryCampaignTypes.stream()
                                .anyMatch(type -> type.equalsIgnoreCase(donation.getPurpose().trim())))
                        .collect(Collectors.toList());
                beneficiaryFundTransactions.forEach(donation -> donorRepository.findById(donation.getDonorId())
                        .ifPresent(donor -> donation.setDonorName(donor.getFullName())));
                model.addAttribute("beneficiaryFundTransactions", beneficiaryFundTransactions);
                model.addAttribute("fundTransactionTotal", beneficiaryFundTransactions.stream()
                        .mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum());
            } catch (Exception ignored) {}
        }
        return "beneficiary/funds_received";
    }

    @GetMapping("/beneficiary/relief-materials")
    public String beneficiaryReliefMaterials(Model model) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/auth/beneficiary/login";
        if (userId != null) {
            try {
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
                List<com.ngo.entity.ReliefMaterialRequest> requests =
                        reliefMaterialRequestRepository.findByBeneficiaryIdOrderByCreatedAtDesc(userId);
                model.addAttribute("materialRequests", requests);
                model.addAttribute("totalRequests", requests.size());
                model.addAttribute("pendingRequests", requests.stream().filter(r -> "pending".equals(r.getStatus())).count());
                model.addAttribute("approvedRequests", requests.stream().filter(r -> "approved".equals(r.getStatus()) || "dispatched".equals(r.getStatus())).count());
                model.addAttribute("deliveredRequests", requests.stream().filter(r -> "delivered".equals(r.getStatus())).count());
            } catch (Exception ignored) {}
        }
        return "beneficiary/relief_materials";
    }

    @PostMapping("/beneficiary/relief-materials/request")
    public String requestReliefMaterials(@RequestParam String materialType,
                                         @RequestParam Integer quantity,
                                         @RequestParam String urgency,
                                         @RequestParam String deliveryAddress,
                                         @RequestParam(required = false) String notes,
                                         RedirectAttributes ra) {
        Long userId = getUserId();
        if (userId == null) return "redirect:/auth/beneficiary/login";
        try {
            if (quantity == null || quantity < 1 || quantity > 1000) {
                throw new IllegalArgumentException("Quantity must be between 1 and 1000.");
            }
            com.ngo.entity.ReliefMaterialRequest request = new com.ngo.entity.ReliefMaterialRequest();
            request.setBeneficiaryId(userId);
            request.setMaterialType(materialType.trim().toLowerCase());
            request.setQuantity(quantity);
            request.setUrgency(urgency.trim().toLowerCase());
            request.setDeliveryAddress(deliveryAddress.trim());
            request.setNotes(notes == null ? null : notes.trim());
            request.setStatus("pending");
            reliefMaterialRequestRepository.save(request);
            ra.addFlashAttribute("successMessage", "Relief material request submitted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Unable to submit request: " + e.getMessage());
        }
        return "redirect:/beneficiary/relief-materials";
    }

    @GetMapping("/beneficiary/campaigns")
    public String beneficiaryCampaigns(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
                model.addAttribute("myReports", disasterReportRepository.findByReportedByBeneficiaryIdOrderByCreatedAtDesc(userId));
            } catch (Exception ignored) {}
        }
        try {
            java.util.List<com.ngo.entity.DisasterReport> allReports = disasterReportRepository.findAllByOrderByCreatedAtDesc();
            allReports.forEach(report -> report.setStatus(report.getStatus() == null
                    ? "pending" : report.getStatus().trim().toLowerCase()));

            java.util.LinkedHashMap<String, com.ngo.entity.DisasterReport> newestOngoingByType = new java.util.LinkedHashMap<>();
            allReports.stream().filter(report -> "active".equals(report.getStatus()) || "pending".equals(report.getStatus()))
                    .forEach(report -> newestOngoingByType.putIfAbsent(
                            report.getDisasterType() == null ? "unknown-" + report.getId() : report.getDisasterType().trim().toLowerCase(), report));
            java.util.List<com.ngo.entity.DisasterReport> visibleReports = new java.util.ArrayList<>(newestOngoingByType.values());
            java.util.List<com.ngo.entity.DisasterReport> completedReports = allReports.stream()
                    .filter(report -> "completed".equals(report.getStatus()) || "resolved".equals(report.getStatus()))
                    .collect(Collectors.toList());

            for (com.ngo.entity.DisasterReport r : visibleReports) {
                Double total = donationRepository.getTotalByPurposeSince(r.getDisasterType(), r.getCreatedAt());
                r.setAmountRaised(total != null ? total : 0.0);
            }
            for (com.ngo.entity.DisasterReport r : completedReports) {
                Double total = donationRepository.getTotalByPurpose(r.getDisasterType());
                r.setAmountRaised(total != null ? total : 0.0);
            }
            model.addAttribute("disasterReports", visibleReports);
            model.addAttribute("completedReports", completedReports);
            // Compute real hero stats
            long totalReports = visibleReports.size();
            long totalFamilies = visibleReports.stream().mapToLong(r -> r.getFamiliesAffected() != null ? r.getFamiliesAffected() : 0).sum();
            long activeCount = visibleReports.stream().filter(r -> "active".equals(r.getStatus())).count();
            long pendingCount = visibleReports.stream().filter(r -> "pending".equals(r.getStatus())).count();
            long completedCount = allReports.stream().filter(r -> "completed".equals(r.getStatus()) || "resolved".equals(r.getStatus())).count();
            model.addAttribute("totalReports", totalReports);
            model.addAttribute("totalFamilies", totalFamilies);
            model.addAttribute("activeCount", activeCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("completedCount", completedCount);
        } catch (Exception ignored) {}
        return "beneficiary/campaigns";
    }

    @PostMapping("/beneficiary/campaigns/report")
    public String submitDisasterReport(
            @RequestParam String disasterType,
            @RequestParam String location,
            @RequestParam String state,
            @RequestParam Integer familiesAffected,
            @RequestParam String disasterDate,
            @RequestParam String contactNumber,
            @RequestParam String severity,
            @RequestParam String description,
            @RequestParam(required = false) Double requiredAmount,
            @RequestParam(required = false) String additionalNotes,
            @RequestParam(required = false) MultipartFile photo,
            RedirectAttributes ra) {
        try {
            DisasterReport report = new DisasterReport();
            report.setDisasterType(disasterType.trim().toLowerCase());
            report.setLocation(location.trim());
            report.setState(state.trim());
            report.setFamiliesAffected(familiesAffected);
            report.setRequiredAmount(requiredAmount);
            report.setDisasterDate(LocalDateTime.parse(disasterDate + "T00:00:00"));
            report.setContactNumber(contactNumber.trim());
            report.setSeverity(severity.trim().toLowerCase());
            report.setDescription(description.trim());
            report.setAdditionalNotes(additionalNotes == null ? null : additionalNotes.trim());
            if (photo != null && !photo.isEmpty()) {
                report.setPhotoBase64(Base64.getEncoder().encodeToString(photo.getBytes()));
            }
            report.setStatus("pending");
            report.setReportedByBeneficiaryId(getUserId());
            report.setCreatedAt(LocalDateTime.now());
            DisasterReport saved = disasterReportRepository.saveAndFlush(report);
            ra.addFlashAttribute("newReportId", saved.getId());
            ra.addFlashAttribute("successMessage", "✅ Disaster report submitted successfully! Our team will review it within 2 hours.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to submit report: " + e.getMessage());
        }
        return "redirect:/beneficiary/campaigns";
    }

    @PostMapping("/beneficiary/campaigns/report/delete")
    @Transactional
    public String deleteDisasterReport(@RequestParam Long reportId, @RequestParam(defaultValue = "/beneficiary/campaigns") String redirectUrl, RedirectAttributes ra) {
        try {
            Long userId = getUserId();
            if (userId != null) {
                disasterReportRepository.findById(reportId).ifPresent(report -> {
                    // Cascade delete linked donations, allocations, utilization reports
                    List<Donation> linkedDonations = donationRepository.findByPurpose(report.getDisasterType());
                    if (linkedDonations != null && !linkedDonations.isEmpty()) {
                        List<Long> donationIds = linkedDonations.stream().map(Donation::getId).collect(Collectors.toList());
                        List<Allocation> linkedAllocations = allocationRepository.findByDonationIdIn(donationIds);
                        if (linkedAllocations != null && !linkedAllocations.isEmpty()) {
                            List<Long> allocIds = linkedAllocations.stream().map(Allocation::getId).collect(Collectors.toList());
                            utilizationReportRepository.deleteByAllocationIdIn(allocIds);
                        }
                        allocationRepository.deleteByDonationIdIn(donationIds);
                        donationRepository.deleteAll(linkedDonations);
                    }
                    
                    // Delete the report itself
                    disasterReportRepository.delete(report);
                    ra.addFlashAttribute("successMessage", "✅ Disaster report and all linked data deleted successfully.");
                });
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "❌ Failed to delete report: " + e.getMessage());
        }
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/beneficiary/notifications")
    public String beneficiaryNotifications(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
                List<Map<String, Object>> notifications = buildBeneficiaryNotifications(userId);
                model.addAttribute("notifications", notifications);
                model.addAttribute("notificationCount", notifications.size());
            } catch (Exception ignored) {}
        }
        return "beneficiary/notifications";
    }

    @GetMapping("/beneficiary/support-tickets")
    public String beneficiarySupportTickets(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
            } catch (Exception ignored) {}
        }
        return "beneficiary/support_tickets";
    }

    @GetMapping("/beneficiary/settings")
    public String beneficiarySettings(Model model) {
        Long userId = getUserId();
        if (userId != null) {
            try {
                Map<String, Object> dash = beneficiaryService.getBeneficiaryDashboard(userId);
                model.addAttribute("beneficiary", dash.get("beneficiary"));
            } catch (Exception ignored) {}
        }
        return "beneficiary/settings";
    }

}
