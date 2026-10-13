package com.careertrack.notification.mail;

public interface VerificationEmailSender {

    void sendVerificationEmail(String recipientEmail, String recipientName,
                               String verificationUrl, long expiresInHours);
}
