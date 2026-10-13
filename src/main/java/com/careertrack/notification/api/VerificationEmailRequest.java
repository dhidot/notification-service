package com.careertrack.notification.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationEmailRequest(
        @NotBlank @Email @Size(max = 254) String recipientEmail,
        @Size(max = 100) String recipientName,
        @NotBlank @Size(max = 2048) String verificationUrl,
        @Min(1) @Max(72) long expiresInHours
) {
}
