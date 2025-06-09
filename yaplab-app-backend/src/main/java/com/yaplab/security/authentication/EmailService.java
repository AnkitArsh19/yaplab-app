package com.yaplab.security.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service layer for handling sending emails to users for welcome, registration and password reset
 * Using log-only approach for testing purposes
 */
@Service
public class EmailService {

    /**
     * Logger for EmailService
     * This logger is used to log various events and errors in the EmailService class.
     * It helps in debugging and tracking the flow of operations related to email management.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final String apiKey;
    private final String namespace;

    public EmailService(
            @Value("${testmail.api.key:dummy-api-key}") String apiKey,
            @Value("${testmail.namespace:dummy-namespace}") String namespace) {
        this.apiKey = apiKey;
        this.namespace = namespace;
        logger.info("EmailService initialized in LOG-ONLY mode (no real emails will be sent)");
    }

    /**
     * This method constructs an HTML email with a reset link and sends it to the user
     * @param to The recipient's email address.
     * @param resetLink  The link to reset the user's password.
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:#f9f9f9;border-radius:12px;">
                    <h2 style="color:#2d3748;">Reset Your Password</h2>
                    <p style="color:#4a5568;">We received a request to reset your password. Click the button below to set a new password:</p>
                    <a href="%s" style="display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;margin:16px 0;">Reset Password</a>
                    <p style="color:#718096;font-size:14px;">If you did not request this, you can safely ignore this email.</p>
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;">
                    <p style="color:#a0aec0;font-size:12px;text-align:center;">&copy; 2025 YapLab</p>
                </div>
                """.formatted(resetLink);

        try {
            sendEmail(to, "Reset Your Password", html);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", to, e);
        }
    }

    /**
     * This method constructs an HTML email with a welcome message after successful registration
     * @param to The recipient's email address.
     * @param userName The name of the user to personalize the email.
     */
    public void sendWelcomeEmail(String to, String userName) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:#e6f7ff;border-radius:12px;">
                <h2 style="color:#2563eb;">Welcome to YapLab, %s!</h2>
                <p style="color:#333;">We're excited to have you on board. Start connecting securely with your friends and colleagues.</p>
                <div style="margin:24px 0;">
                    <a href="https://your-app-url.com/login" style="display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;">Get Started</a>
                </div>
                <p style="color:#718096;font-size:14px;">If you have any questions, just reply to this email—we're here to help!</p>
                <hr style="border:none;border-top:1px solid #b3e0ff;margin:24px 0;">
                <p style="color:#a0aec0;font-size:12px;text-align:center;">&copy; 2025 Yap Lab</p>
            </div>
            """.formatted(userName);

        try {
            sendEmail(to, "Welcome to YapLab!", html);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}", to, e);
        }
    }

    /**
     * This method constructs an HTML email with a verification link.
     * @param to The recipient's email address.
     * @param verificationLink  The link to verify the user's email address.
     */
    public void sendVerificationEmail(String to, String verificationLink) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:#f0fff4;border-radius:12px;">
                <h2 style="color:#22c55e;">Verify Your Email Address</h2>
                <p style="color:#4a5568;">Thank you for signing up! Please click the button below to verify your email address:</p>
                <div style="margin:24px 0;">
                    <a href="%s" style="display:inline-block;padding:12px 24px;background:#22c55e;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;">Verify Email</a>
                </div>
                <p style="color:#718096;font-size:14px;">If you did not sign up for this service, you can safely ignore this email.</p>
                <hr style="border:none;border-top:1px solid #a7f3d0;margin:24px 0;">
                <p style="color:#a0aec0;font-size:12px;text-align:center;">&copy; 2025 YapLab App</p>
            </div>
            """.formatted(verificationLink);

        try {
            sendEmail(to, "Verify Your Email Address", html);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", to, e);
        }
    }

    /**
     * Private helper method that logs email details instead of actually sending emails
     * This is used for testing purposes only
     *
     * @param to The recipient's email address
     * @param subject The email subject
     * @param htmlContent The HTML content of the email
     * @throws Exception if there is an error in processing
     */
    private void sendEmail(String to, String subject, String htmlContent) throws Exception {
        // Extract links from HTML content for easy testing
        String extractedLink = extractLinkFromHtml(htmlContent);

        // Format a nice log message with all the details
        logger.info("\n" +
                        "╔════════════════════════════════════════════════════════════════════╗\n" +
                        "║                       SIMULATED EMAIL SENT                         ║\n" +
                        "╠════════════════════════════════════════════════════════════════════╣\n" +
                        "║ TO:      {}\n" +
                        "║ SUBJECT: {}\n" +
                        "╠════════════════════════════════════════════════════════════════════╣\n" +
                        "║ LINK:    {}\n" +
                        "╚════════════════════════════════════════════════════════════════════╝",
                to, subject, extractedLink);

        // For debugging purposes, also log the full HTML content at debug level
        logger.debug("Email HTML content: {}", htmlContent);

        // Simulate successful email sending
        logger.info("Email successfully 'sent' to {} (simulated)", to);
    }

    /**
     * Extracts the first link (href) from HTML content
     *
     * @param html The HTML content to extract links from
     * @return The first link found in the HTML
     */
    private String extractLinkFromHtml(String html) {
        Pattern pattern = Pattern.compile("href=[\"'](.*?)[\"']");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "No link found in email content";
    }
}