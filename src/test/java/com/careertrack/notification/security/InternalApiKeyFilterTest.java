package com.careertrack.notification.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalApiKeyFilterTest {

    @Test
    void failsClosedWhenSecretIsMissingOrTooShort() {
        assertThrows(IllegalStateException.class, () -> new InternalApiKeyFilter(""));
        assertThrows(IllegalStateException.class, () -> new InternalApiKeyFilter("short"));
    }

    @Test
    void acceptsStrongConfiguredSecret() {
        assertDoesNotThrow(() -> new InternalApiKeyFilter("this-secret-has-at-least-32-characters"));
    }

    @Test
    void rejectsMissingKeyEvenWhenServletPathIsEmpty() throws Exception {
        var filter = new InternalApiKeyFilter("this-secret-has-at-least-32-characters");
        var request = new MockHttpServletRequest("POST", "/internal/notifications/email-verification");
        var response = new MockHttpServletResponse();
        var reachedController = new AtomicBoolean(false);

        assertEquals("", request.getServletPath());
        filter.doFilter(request, response, (req, res) -> reachedController.set(true));

        assertEquals(401, response.getStatus());
        assertFalse(reachedController.get());
    }

    @Test
    void permitsCorrectKeyOnInternalEndpoint() throws Exception {
        String key = "this-secret-has-at-least-32-characters";
        var filter = new InternalApiKeyFilter(key);
        var request = new MockHttpServletRequest("POST", "/internal/notifications/email-verification");
        request.addHeader("X-Internal-Api-Key", key);
        var response = new MockHttpServletResponse();
        var reachedController = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> reachedController.set(true));

        assertTrue(reachedController.get());
    }

    @Test
    void permitsHealthWithoutKeyButProtectsOtherPaths() throws Exception {
        var filter = new InternalApiKeyFilter("this-secret-has-at-least-32-characters");
        var healthRequest = new MockHttpServletRequest("GET", "/actuator/health");
        var healthResponse = new MockHttpServletResponse();
        var healthReached = new AtomicBoolean(false);

        filter.doFilter(healthRequest, healthResponse, (req, res) -> healthReached.set(true));
        assertTrue(healthReached.get());

        var otherRequest = new MockHttpServletRequest("GET", "/api/unmapped");
        var otherResponse = new MockHttpServletResponse();
        var otherReached = new AtomicBoolean(false);
        filter.doFilter(otherRequest, otherResponse, (req, res) -> otherReached.set(true));

        assertEquals(401, otherResponse.getStatus());
        assertFalse(otherReached.get());
    }
}
