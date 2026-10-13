package com.careertrack.notification.api;

import com.careertrack.notification.mail.EmailDeliveryException;
import com.careertrack.notification.mail.VerificationEmailSender;
import com.careertrack.notification.security.InternalApiKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VerificationEmailController.class,
        properties = "careertrack.internal.api-key=test-only-internal-api-key-32-chars-minimum")
@Import(InternalApiKeyFilter.class)
class VerificationEmailControllerTest {

    private static final String PATH = "/internal/notifications/email-verification";
    private static final String SECRET = "test-only-internal-api-key-32-chars-minimum";
    private static final String BODY = """
            {"recipientEmail":"user@example.test","recipientName":"Career User",
            "verificationUrl":"https://app.example.test/verify-email#token=highly-sensitive-token",
            "expiresInHours":24}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VerificationEmailSender emailSender;

    @Test
    void acceptsAuthenticatedRequestAndForwardsToEmailSender() throws Exception {
        mvc.perform(post(PATH).header("X-Internal-Api-Key", SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNoContent());

        verify(emailSender).sendVerificationEmail("user@example.test", "Career User",
                "https://app.example.test/verify-email#token=highly-sensitive-token", 24);
    }

    @Test
    void rejectsMissingApiKeyBeforeInvokingSender() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(emailSender);
    }

    @Test
    void rejectsInvalidApiKeyBeforeInvokingSender() throws Exception {
        mvc.perform(post(PATH).header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(emailSender);
    }

    @Test
    void rejectsInvalidEmailWithoutSending() throws Exception {
        String body = BODY.replace("user@example.test", "not-an-email");
        mvc.perform(post(PATH).header("X-Internal-Api-Key", SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(emailSender);
    }

    @Test
    void rejectsInvalidExpiryWithoutSending() throws Exception {
        String body = BODY.replace("\"expiresInHours\":24", "\"expiresInHours\":0");
        mvc.perform(post(PATH).header("X-Internal-Api-Key", SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(emailSender);
    }

    @Test
    void hidesRawTokenWhenSmtpFails() throws Exception {
        doThrow(new EmailDeliveryException()).when(emailSender)
                .sendVerificationEmail(anyString(), anyString(), anyString(), anyLong());

        mvc.perform(post(PATH).header("X-Internal-Api-Key", SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EMAIL_DELIVERY_FAILED"))
                .andExpect(content().string(not(containsString("highly-sensitive-token"))));
    }

    @Test
    void respondsWithGenericBadRequestWhenLinkIsRejected() throws Exception {
        doThrow(new IllegalArgumentException("Invalid verification URL"))
                .when(emailSender).sendVerificationEmail(anyString(), anyString(), anyString(), anyLong());

        mvc.perform(post(PATH).header("X-Internal-Api-Key", SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(content().string(not(containsString("highly-sensitive-token"))));
    }

    @Test
    void hidesMalformedJsonFromErrorResponse() throws Exception {
        mvc.perform(post(PATH).header("X-Internal-Api-Key", SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verificationUrl\": malformed}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(emailSender);
    }
}
