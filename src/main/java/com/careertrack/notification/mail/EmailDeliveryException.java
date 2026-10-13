package com.careertrack.notification.mail;

public final class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException() {
        super("Verification email delivery failed");
    }
}
