package com.example.orderservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;
    
    @Value("${spring.mail.username:no-reply@example.com}")
    private String senderEmail;
    
    @Value("${email.sending.enabled:false}")
    private boolean sendingEnabled;
    
    /**
     * Sends a simple text email
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     */
    public void sendEmail(String to, String subject, String body) {
        // Skip actual sending if disabled (e.g., in dev/test environments)
        if (!sendingEnabled) {
            logEmail(to, subject, body, null);
            return;
        }
        
        // Check if the recipient email is set
        if (to == null || to.trim().isEmpty()) {
            log.warn("Cannot send email: recipient address is empty (subject={})", subject);
            return;
        }
        
        // Check if email credentials are set
        if (senderEmail == null || senderEmail.isEmpty() || senderEmail.equals("no-reply@example.com")) {
            log.warn("Cannot send email: sender email credentials not configured properly (senderEmail={})",
                    senderEmail);
            logEmail(to, subject, body, null);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            javaMailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            // Fall back to logging the email for development/debugging
            logEmail(to, subject, body, null);
        }
    }

    /**
     * Sends an email with a file attachment
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     * @param attachment file to attach
     */
    public void sendInvoiceEmail(String to, String subject, String body, File attachment) {
        // Skip actual sending if disabled (e.g., in dev/test environments)
        if (!sendingEnabled) {
            logEmail(to, subject, body, attachment);
            log.info("Email sending is disabled. Invoice would have been sent to {}", to);
            return;
        }
        
        // Check if the recipient email is valid
        if (to == null || to.trim().isEmpty()) {
            log.warn("Cannot send invoice email: recipient address is empty (subject={})", subject);
            return;
        }
        
        // Check if attachment exists
        if (attachment == null || !attachment.exists()) {
            log.warn("Cannot send invoice email: attachment file does not exist: {}",
                    attachment == null ? "null" : attachment.getAbsolutePath());
            logEmail(to, subject, body, null);
            return;
        }
        
        // Check if email credentials are set
        if (senderEmail == null || senderEmail.isEmpty() || senderEmail.equals("no-reply@example.com")) {
            log.warn("Cannot send invoice email: sender email credentials not configured properly (senderEmail={})",
                    senderEmail);
            logEmail(to, subject, body, attachment);
            return;
        }
        
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true indicates multipart message
            
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // false for plain text, true for HTML
            
            // Add the attachment
            FileSystemResource file = new FileSystemResource(attachment);
            helper.addAttachment(attachment.getName(), file);
            
            log.info("Attempting to send email with attachment to {} using sender {}", to, senderEmail);
            
            javaMailSender.send(message);
            log.info("Email with attachment sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage(), e);
            String msg = e.getMessage() != null ? e.getMessage() : "";
            boolean authError =
                    msg.contains("Authentication failed") ||
                    msg.contains("Username and Password not accepted") ||
                    msg.contains("Application-specific password required");

            if (authError) {
                log.error("=====================================================");
                log.error("GMAIL AUTHENTICATION ERROR DETECTED");
                log.error("This is likely because you need to use an App Password instead of your regular password.");
                log.error("1. Enable 2-Step Verification on your Google Account: https://myaccount.google.com/security");
                log.error("2. Generate an App Password: https://myaccount.google.com/apppasswords");
                log.error("3. Use that App Password in your application configuration");
                log.error("=====================================================");
            }
            
            // Fall back to logging the email for development/debugging so we
            // never lose the intent of the send when SMTP is unavailable.
            logEmail(to, subject, body, attachment);

            // Surface a runtime exception so that upstream flows (e.g., invoice
            // jobs) can apply their own retry/backoff policies for both
            // transient (network/throttling) and non-transient (auth) errors.
            if (authError) {
                throw new RuntimeException("Email authentication error while sending invoice email", e);
            } else {
                throw new RuntimeException("SMTP error while sending invoice email", e);
            }
        }
    }
    
    /**
     * Fallback method that logs email details when actual sending fails
     * This is useful during development or when email server is not configured
     */
    private void logEmail(String to, String subject, String body, File attachment) {
        log.info("-------------------------");
        log.info("[EMAIL FALLBACK LOG] Email details:");
        log.info("To: {}", to);
        log.info("From: {}", senderEmail);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        if (attachment != null) {
            log.info("Attachment: {}", attachment.getAbsolutePath());
            log.info("Attachment exists: {}", attachment.exists());
        }
        log.info("-------------------------");
        
        // For local testing purposes - create a local text file with the email content
        if (attachment != null && attachment.exists()) {
            try {
                String fileName = "email_log_" + System.currentTimeMillis() + ".txt";
                File emailLog = new File(attachment.getParentFile(), fileName);
                java.io.FileWriter writer = new java.io.FileWriter(emailLog);
                writer.write("To: " + to + "\n");
                writer.write("From: " + senderEmail + "\n");
                writer.write("Subject: " + subject + "\n");
                writer.write("Body: " + body + "\n");
                writer.write("Attachment: " + attachment.getAbsolutePath() + "\n");
                writer.close();
                log.info("Email log written to: {}", emailLog.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Error writing email log: {}", e.getMessage(), e);
            }
        }
    }
}
