package com.ngo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.ngo.exception.EmailDeliveryException;
import com.ngo.entity.DisasterReport;
import jakarta.mail.internet.MimeMessage;
import java.util.Collection;
import java.util.Base64;
import java.util.Properties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.enabled:false}")
    private volatile boolean enabled;

    @Value("${app.mail.from:}")
    private volatile String from;

    @Value("${jwt.secret}")
    private String credentialEncryptionSecret;

    private final Path persistedSettings = Path.of("config", "mail-settings.dat");

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @PostConstruct
    void restorePersistedSettings() {
        if (!Files.exists(persistedSettings) || !(mailSender instanceof JavaMailSenderImpl sender)) return;
        try {
            byte[] payload = Base64.getDecoder().decode(Files.readString(persistedSettings, StandardCharsets.UTF_8).trim());
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, 12, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, iv));
            String[] values = new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8).split("\\n", 2);
            if (values.length == 2 && !values[0].isBlank() && !values[1].isBlank()) {
                applyGmailSettings(sender, values[0], values[1]);
                from = values[0];
                enabled = true;
                log.info("Restored persistent Gmail configuration for {}", from);
            }
        } catch (Exception exception) {
            enabled = false;
            log.error("Unable to restore encrypted Gmail settings: {}", exception.getMessage());
        }
    }

    public MailConnectionStatus getConnectionStatus() {
        if (!enabled) {
            return new MailConnectionStatus(false, "Not connected", "Email delivery is disabled. Set MAIL_ENABLED=true to connect Gmail.");
        }
        if (from == null || from.isBlank()) {
            return new MailConnectionStatus(false, "Not connected", "The Gmail address is not configured.");
        }
        if (!(mailSender instanceof JavaMailSenderImpl sender)) {
            return new MailConnectionStatus(false, "Not connected", "The Gmail SMTP sender is unavailable.");
        }
        try {
            sender.testConnection();
            return new MailConnectionStatus(true, "Connected", "Gmail SMTP authentication succeeded.");
        } catch (Exception exception) {
            log.warn("Gmail connection check failed: {}", exception.getMessage());
            return new MailConnectionStatus(false, "Not connected", "Gmail SMTP authentication failed. Check the address and App Password.");
        }
    }

    public synchronized MailConnectionStatus configureAndConnect(String username, String appPassword, boolean mailEnabled) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanPassword = appPassword == null ? "" : appPassword.replace(" ", "").trim();
        if (!mailEnabled) {
            enabled = false;
            try { Files.deleteIfExists(persistedSettings); }
            catch (Exception exception) { log.warn("Unable to remove saved Gmail settings: {}", exception.getMessage()); }
            return getConnectionStatus();
        }
        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            return new MailConnectionStatus(false, "Not connected", "Enter both the Gmail address and Google App Password.");
        }
        if (!(mailSender instanceof JavaMailSenderImpl sender)) {
            return new MailConnectionStatus(false, "Not connected", "The Gmail SMTP sender is unavailable.");
        }

        applyGmailSettings(sender, cleanUsername, cleanPassword);
        from = cleanUsername;
        enabled = true;
        try {
            sender.testConnection();
            sendPlainText(cleanUsername, "Gmail connected successfully",
                    "Secure Disaster Relief Platform is now connected to this Gmail account. Email notifications are enabled.");
            persistSettings(cleanUsername, cleanPassword);
            return new MailConnectionStatus(true, "Connected", "Gmail is connected and a test email was sent successfully.");
        } catch (Exception exception) {
            enabled = false;
            log.error("Unable to connect or send the Gmail test message: {}", exception.getMessage(), exception);
            return new MailConnectionStatus(false, "Not connected",
                    "Gmail could not send the test email. Verify 2-Step Verification and the 16-character App Password.");
        }
    }

    private void applyGmailSettings(JavaMailSenderImpl sender, String username, String password) {
        sender.setUsername(username); sender.setPassword(password); sender.setHost("smtp.gmail.com");
        sender.setPort(587); sender.setProtocol("smtp"); sender.setDefaultEncoding("UTF-8");
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true"); properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true"); properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000"); properties.put("mail.smtp.writetimeout", "10000");
    }

    private void persistSettings(String username, String password) throws Exception {
        Files.createDirectories(persistedSettings.getParent());
        byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal((username + "\n" + password).getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length); System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        Files.writeString(persistedSettings, Base64.getEncoder().encodeToString(payload), StandardCharsets.UTF_8);
    }

    private SecretKeySpec encryptionKey() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(credentialEncryptionSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    public String getConfiguredAddress() {
        return from == null || from.isBlank() ? "Not configured" : from;
    }

    public record MailConnectionStatus(boolean connected, String label, String detail) {}

    public void sendTestEmail(String recipient) {
        try {
            requireMailEnabled();
            sendPlainText(recipient, "Gmail SMTP test successful",
                    "Your Secure Disaster Relief Platform email service is configured and working.");
            log.info("Test email sent successfully to {}", recipient);
        } catch (Exception exception) {
            throw deliveryFailure(recipient, exception);
        }
    }

    public void sendVerificationEmail(String recipient, String recipientName, String verificationUrl) {
        sendTemplate(recipient, "Verify your email address", "email/verification",
                Map.of("name", safe(recipientName, "User"), "actionUrl", safe(verificationUrl, "#")));
    }

    public void sendPasswordResetEmail(String recipient, String recipientName, String resetUrl) {
        sendTemplate(recipient, "Reset your password", "email/password-reset",
                Map.of("name", safe(recipientName, "User"), "actionUrl", safe(resetUrl, "#")));
    }

    public void sendPasswordResetOtpEmail(String recipient, String recipientName, String otp) {
        sendTemplate(recipient, "Your password reset OTP", "email/password-reset-otp",
                Map.of("name", safe(recipientName, "Account Holder"), "otp", safe(otp, "------")));
    }

    public void sendDonationReceiptEmail(String recipient, String donorName, String receiptNumber,
                                         BigDecimal amount, LocalDateTime donatedAt) {
        String formattedAmount = amount == null ? "0.00" : String.format(Locale.ENGLISH, "%,.2f", amount);
        String formattedDate = (donatedAt == null ? LocalDateTime.now() : donatedAt)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH));
        sendTemplate(recipient, "Donation receipt " + safe(receiptNumber, ""), "email/donation-receipt",
                Map.of("name", safe(donorName, "Donor"), "receiptNumber", safe(receiptNumber, "Pending"),
                        "amount", formattedAmount, "donatedAt", formattedDate));
    }

    public void sendNGOApprovalEmail(String recipient, String ngoName, String dashboardUrl) {
        sendTemplate(recipient, "Your NGO account has been approved", "email/ngo-approval",
                Map.of("name", safe(ngoName, "NGO Partner"), "actionUrl", safe(dashboardUrl, "#")));
    }

    private void sendTemplate(String recipient, String subject, String template, Map<String, Object> variables) {
        try {
            requireMailEnabled();
            Context context = new Context(Locale.ENGLISH);
            context.setVariables(variables);
            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setReplyTo(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("HTML email '{}' sent successfully to {}", template, recipient);
        } catch (Exception exception) {
            throw deliveryFailure(recipient, exception);
        }
    }

    private void requireMailEnabled() {
        if (!enabled || from == null || from.isBlank()) {
            throw new IllegalStateException("Email is disabled or MAIL_USERNAME is not configured.");
        }
    }

    private EmailDeliveryException deliveryFailure(String recipient, Exception exception) {
        log.error("Email delivery failed for {}: {}", recipient, exception.getMessage(), exception);
        return new EmailDeliveryException("Unable to send email. Check the Gmail SMTP credentials and server logs.", exception);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void sendPlainText(String recipient, String subject, String body) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setReplyTo(from);
        mail.setTo(recipient.trim());
        mail.setSubject(subject == null || subject.isBlank() ? "Platform notification" : subject.trim());
        mail.setText(body == null ? "" : body);
        mailSender.send(mail);
    }

    @Async
    public void sendStatusEmail(String recipient, String subject, String message) {
        if (!enabled || recipient == null || recipient.isBlank() || from == null || from.isBlank()) {
            log.debug("Email skipped because Gmail SMTP is disabled or incomplete.");
            return;
        }
        try {
            sendPlainText(recipient, subject, "Secure Disaster Relief Platform\n\n" + message
                    + "\n\nThis is an automated notification. Please do not reply.");
            log.info("Email sent successfully to {} with subject '{}'", recipient, subject);
        } catch (Exception exception) {
            log.error("Unable to send email to {}: {}", recipient, exception.getMessage());
        }
    }

    @Async
    public void sendEmergencyAlert(Collection<String> recipients, String disasterType, String location,
                                   String severity, Integer families, Double requiredAmount, String description,
                                   String photoBase64) {
        if (!enabled || recipients == null || recipients.isEmpty() || from == null || from.isBlank()) return;
        String type = HtmlUtils.htmlEscape(disasterType == null ? "Disaster" : disasterType);
        String place = HtmlUtils.htmlEscape(location == null ? "Affected area" : location);
        String level = HtmlUtils.htmlEscape(severity == null ? "Urgent" : severity);
        String details = HtmlUtils.htmlEscape(description == null ? "Emergency relief support is required." : description);
        String amount = String.format("%,.0f", requiredAmount == null ? 0.0 : requiredAmount);
        String html = "<div style='font-family:Arial,sans-serif;background:#f1f5f9;padding:28px'>"
                + "<div style='max-width:620px;margin:auto;background:white;border-radius:16px;overflow:hidden;box-shadow:0 8px 25px #cbd5e1'>"
                + "<div style='background:linear-gradient(135deg,#991b1b,#ef4444);color:white;padding:26px'>"
                + "<div style='font-size:13px;font-weight:bold;letter-spacing:1px'>EMERGENCY ALERT</div>"
                + "<h1 style='margin:8px 0 4px;font-size:25px'>" + type + " in " + place + "</h1>"
                + "<div style='opacity:.9'>Secure Disaster Relief Platform</div></div>"
                + "<img src='cid:alertImage' alt='Disaster situation' style='width:100%;height:260px;object-fit:cover;display:block'>"
                + "<div style='padding:26px'><div style='display:inline-block;background:#fee2e2;color:#b91c1c;padding:7px 12px;border-radius:20px;font-weight:bold'>"
                + level + " priority</div><p style='color:#475569;line-height:1.7'>" + details + "</p>"
                + "<table style='width:100%;border-collapse:separate;border-spacing:8px'><tr>"
                + "<td style='background:#f8fafc;padding:14px;border-radius:10px'><small style='color:#64748b'>FAMILIES AFFECTED</small><br><b>" + (families == null ? 0 : families) + "</b></td>"
                + "<td style='background:#f8fafc;padding:14px;border-radius:10px'><small style='color:#64748b'>FUNDS REQUIRED</small><br><b>₹" + amount + "</b></td></tr></table>"
                + "<p style='color:#64748b;font-size:13px'>Log in to the platform to view the ongoing disaster situation and provide support.</p></div>"
                + "<div style='background:#0f172a;color:#94a3b8;padding:16px;text-align:center;font-size:12px'>Automated emergency notification</div></div></div>";
        recipients.stream().filter(address -> address != null && !address.isBlank()).distinct().forEach(address -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(from); helper.setReplyTo(from); helper.setTo(address); helper.setSubject("Emergency Alert: " + type + " in " + place);
                helper.setText(html, true);
                if (photoBase64 != null && !photoBase64.isBlank()) {
                    String encoded = photoBase64.contains(",") ? photoBase64.substring(photoBase64.indexOf(',') + 1) : photoBase64;
                    helper.addInline("alertImage", new ByteArrayResource(Base64.getDecoder().decode(encoded)), "image/jpeg");
                } else {
                    String imageName = type.equalsIgnoreCase("earthquake") ? "earthquake_disaster.png"
                            : (type.equalsIgnoreCase("cyclone") ? "cyclone_disaster.png" : "flood_disaster.png");
                    helper.addInline("alertImage", new ClassPathResource("static/images/" + imageName), "image/png");
                }
                mailSender.send(message);
                log.info("Emergency email sent successfully to {}", address);
            } catch (Exception exception) { log.error("Emergency email failed for {}: {}", address, exception.getMessage()); }
        });
    }

    @Async
    public void sendEmergencyAlert(Collection<String> recipients, DisasterReport report) {
        if (!enabled || recipients == null || recipients.isEmpty() || report == null) return;
        recipients.stream().filter(address -> address != null && !address.isBlank()).map(String::trim).distinct().forEach(address -> {
            try {
                Context context = new Context(Locale.ENGLISH);
                context.setVariable("report", report);
                context.setVariable("requiredAmount", String.format(Locale.ENGLISH, "%,.2f", report.getRequiredAmount() == null ? 0.0 : report.getRequiredAmount()));
                context.setVariable("disasterDate", report.getDisasterDate() == null ? "Not specified" : report.getDisasterDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)));
                String html = templateEngine.process("email/emergency-alert", context);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(from); helper.setReplyTo(from); helper.setTo(address);
                helper.setSubject("Emergency Alert: " + safe(report.getDisasterType(), "Disaster") + " in " + safe(report.getLocation(), "Affected area"));
                helper.setText(html, true);
                if (report.getPhotoBase64() != null && !report.getPhotoBase64().isBlank()) {
                    String encoded = report.getPhotoBase64().contains(",") ? report.getPhotoBase64().substring(report.getPhotoBase64().indexOf(',') + 1) : report.getPhotoBase64();
                    helper.addInline("alertImage", new ByteArrayResource(Base64.getDecoder().decode(encoded)), "image/jpeg");
                } else {
                    String type = safe(report.getDisasterType(), "flood");
                    String imageName = type.equalsIgnoreCase("earthquake") ? "earthquake_disaster.png" : (type.equalsIgnoreCase("cyclone") ? "cyclone_disaster.png" : "flood_disaster.png");
                    helper.addInline("alertImage", new ClassPathResource("static/images/" + imageName), "image/png");
                }
                mailSender.send(message);
                log.info("Emergency alert #{} sent to {}", report.getId(), address);
            } catch (Exception exception) {
                log.error("Emergency alert #{} failed for {}: {}", report.getId(), address, exception.getMessage(), exception);
            }
        });
    }
}
