package com.careertrack.notification.api;

import com.careertrack.notification.mail.EmailDeliveryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = VerificationEmailController.class)
public final class NotificationApiExceptionHandler {

    // Never echo request bodies or validation errors: the URL may contain a token.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> invalidRequest(Exception ignored) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST"));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ErrorResponse> deliveryFailed(EmailDeliveryException ignored) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("EMAIL_DELIVERY_FAILED"));
    }

    public record ErrorResponse(String code) {
    }
}
