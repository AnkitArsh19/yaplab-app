package com.yaplab.security.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public EmailService() {
        logger.info("EmailService initialized in LOG-ONLY mode (no real emails will be sent)");
    }

    /**
     * Generic method to send notification emails
     * @param to The recipient's email address
     * @param subject The email subject
     * @param title The main title of the email
     * @param message The main message content
     * @param buttonText Optional button text (can be null)
     * @param buttonLink Optional button link (can be null)
     * @param backgroundColor Background color for the email container
     * @param titleColor Color for the title
     */
    public void sendNotificationEmail(String to, String subject, String title, String message, 
                                     String buttonText, String buttonLink, String backgroundColor, String titleColor) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append(String.format("""
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:%s;border-radius:12px;">
                <h2 style="color:%s;">%s</h2>
                <p style="color:#4a5568;">%s</p>
            """, backgroundColor, titleColor, title, message));
        
        if (buttonText != null && buttonLink != null) {
            htmlBuilder.append(String.format("""
                <div style="margin:24px 0;">
                    <a href="%s" style="display:inline-block;padding:12px 24px;background:%s;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;">%s</a>
                </div>
                """, buttonLink, titleColor, buttonText));
        }
        
        htmlBuilder.append("""
                <hr style="border:none;border-top:1px solid rgba(0,0,0,0.1);margin:24px 0;">
                <p style="color:#a0aec0;font-size:12px;text-align:center;">&copy; 2025 YapLab</p>
            </div>
            """);

        try {
            sendEmail(to, subject, htmlBuilder.toString());
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, subject, e);
        }
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        sendNotificationEmail(to, "Reset Your Password", "Reset Your Password", 
            "We received a request to reset your password. Click the button below to set a new password:",
            "Reset Password", resetLink, "#f9f9f9", "#2563eb");
    }

    /**
     * Send welcome email
     */
    public void sendWelcomeEmail(String to, String userName) {
        sendNotificationEmail(to, "Welcome to YapLab!", "Welcome to YapLab, " + userName + "!", 
            "We're excited to have you on board. Start connecting securely with your friends and colleagues.",
            "Get Started", "https://your-app-url.com/login", "#e6f7ff", "#2563eb");
    }

    /**
     * Send email verification
     */
    public void sendVerificationEmail(String to, String verificationLink) {
        sendNotificationEmail(to, "Verify Your Email Address", "Verify Your Email Address", 
            "Thank you for signing up! Please click the button below to verify your email address:",
            "Verify Email", verificationLink, "#f0fff4", "#22c55e");
    }

    /**
     * Send email change verification
     */
    public void sendEmailChangeVerification(String newEmail, String verificationLink, String userName) {
        sendNotificationEmail(newEmail, "Verify Your Email Change - YapLab", "Verify Your Email Change", 
            "Hello " + userName + ", we received a request to change your email address to this email. Please click the button below to confirm this change:",
            "Verify Email Change", verificationLink, "#fef3c7", "#d97706");
    }

    /**
     * Send notification when mobile number is changed
     */
    public void sendMobileNumberChangeNotification(String to, String userName, String newMobileNumber) {
        String maskedNumber = newMobileNumber != null ? 
            "****" + newMobileNumber.substring(Math.max(0, newMobileNumber.length() - 4)) : "Removed";
        String message = String.format("Hello %s, your mobile number has been successfully updated to: %s. If you did not make this change, please contact our support team immediately.", 
                                      userName, maskedNumber);
        sendNotificationEmail(to, "Mobile Number Updated - YapLab", "Mobile Number Updated", 
            message, null, null, "#dbeafe", "#1d4ed8");
    }

    /**
     * Send notification when password is changed
     */
    public void sendPasswordChangeNotification(String to, String userName) {
        String message = String.format("Hello %s, your password has been successfully changed. If you did not make this change, please reset your password immediately and contact our support team.", userName);
        sendNotificationEmail(to, "Password Changed - YapLab", "Password Successfully Changed", 
            message, "Reset Password", "http://localhost:5173/auth/forgot-password", "#dcfce7", "#16a34a");
    }

    /**
     * Send notification when account is deleted
     */
    public void sendAccountDeletionNotification(String to, String userName) {
        String message = String.format("Hello %s, your YapLab account has been permanently deleted as requested. " +
            "We're sad to see you go! All your data has been securely removed from our systems. " +
            "If you ever decide to return, we'd be happy to have you back. Thank you for being part of our community.", userName);
        sendNotificationEmail(to, "Account Deleted - YapLab", "Account Successfully Deleted", 
            message, "Join Us Again", "http://localhost:5173/auth/register", "#fef3c7", "#f59e0b");
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
        logger.info("""
                        
                        ╔════════════════════════════════════════════════════════════════════╗
                        ║                       SIMULATED EMAIL SENT                         ║
                        ╠════════════════════════════════════════════════════════════════════╣
                        ║ TO:      {}
                        ║ SUBJECT: {}
                        ╠════════════════════════════════════════════════════════════════════╣
                        ║ LINK:    {}
                        ╚════════════════════════════════════════════════════════════════════╝""",
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