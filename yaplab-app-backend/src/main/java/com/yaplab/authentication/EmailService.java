package com.yaplab.authentication;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final Resend resend;

    public EmailService(@Value("${resend.api.key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendPasswordResetEmail(String to, String resetLink) throws ResendException {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:#f9f9f9;border-radius:12px;">
                    <h2 style="color:#2d3748;">Reset Your Password</h2>
                    <p style="color:#4a5568;">We received a request to reset your password. Click the button below to set a new password:</p>
                    <a href="%s" style="display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;margin:16px 0;">Reset Password</a>
                    <p style="color:#718096;font-size:14px;">If you did not request this, you can safely ignore this email.</p>
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;">
                    <p style="color:#a0aec0;font-size:12px;text-align:center;">&copy; 2024 Secure Messaging App</p>
                </div>
                """.formatted(resetLink);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Secure Messaging <onboarding@resend.dev>") // Use your verified sender
                .to(to)
                .subject("Reset Your Password")
                .html(html)
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
        } catch (ResendException e) {
            logger.error("Failed to send password reset email to {}", to, e);
        }

    }

    public void sendWelcomeEmail(String to, String userName) {
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;background:#e6f7ff;border-radius:12px;">
                <h2 style="color:#2563eb;">Welcome to Secure Messaging, %s!</h2>
                <p style="color:#333;">We're excited to have you on board. Start connecting securely with your friends and colleagues.</p>
                <div style="margin:24px 0;">
                    <a href="https://your-app-url.com/login" style="display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px;font-weight:bold;">Get Started</a>
                </div>
                <p style="color:#718096;font-size:14px;">If you have any questions, just reply to this email—we're here to help!</p>
                <hr style="border:none;border-top:1px solid #b3e0ff;margin:24px 0;">
                <p style="color:#a0aec0;font-size:12px;text-align:center;">&copy; 2024 Secure Messaging App</p>
            </div>
            """.formatted(userName);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Secure Messaging <onboarding@resend.dev>")
                .to(to)
                .subject("Welcome to Secure Messaging!")
                .html(html)
                .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
        } catch (ResendException e) {
            logger.error("Failed to send welcome email to {}", to, e);
        }
    }
}
