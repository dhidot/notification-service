package com.careertrack.notification.api;

import com.careertrack.notification.mail.VerificationEmailSender;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public final class VerificationEmailController {

    private final VerificationEmailSender emailSender;

    public VerificationEmailController(VerificationEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @PostMapping("/email-verification")
    public ResponseEntity<Void> sendVerificationEmail(@Valid @RequestBody VerificationEmailRequest request) {
        emailSender.sendVerificationEmail(
                request.recipientEmail(),
                request.recipientName(),
                request.verificationUrl(),
                request.expiresInHours());
        // SMTP accepted the message. This does NOT guarantee inbox delivery.
        return ResponseEntity.noContent().build();
    }
}
