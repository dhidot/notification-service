package com.careertrack.notification.mail;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpVerificationEmailSenderTest {

    private JavaMailSender mailSender;
    private SmtpVerificationEmailSender service;
    private MimeMessage message;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new SmtpVerificationEmailSender(mailSender, "no-reply@careertrack.test");
        message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    void sendsHtmlAndPlainTextWithoutEmbeddingUnescapedHtml() throws Exception {
        String url = "https://careertrack.example/verify-email#token=abc&source=email";
        service.sendVerificationEmail("newuser@example.test", "<script>alert(1)</script>", url, 24);

        verify(mailSender).send(message);
        // The mocked JavaMailSender does not finalize MIME headers automatically.
        message.saveChanges();
        assertEquals("Aktivasi Akun CareerTrack", message.getSubject());
        assertEquals("newuser@example.test", message.getAllRecipients()[0].toString());
        assertEquals("no-reply@careertrack.test", message.getFrom()[0].toString());
        String html = findContent(message, "text/html");
        String plain = findContent(message, "text/plain");
        assertNotNull(html);
        assertNotNull(plain);
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("abc&amp;source=email"));
        assertTrue(plain.contains(url));
    }

    @Test
    void wrapsSmtpFailureWithoutLeakingToken() {
        doThrow(new MailSendException("SMTP error with token: secret-token"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException failure = assertThrows(EmailDeliveryException.class,
                () -> service.sendVerificationEmail("newuser@example.test", "New User",
                        "https://careertrack.example/verify-email#token=secret-token", 24));

        assertEquals("Verification email delivery failed", failure.getMessage());
        assertNull(failure.getCause());
    }

    @Test
    void rejectsUntrustedLinkProtocolsAndNonlocalHttp() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendVerificationEmail("a@example.test", "A", "javascript:alert(1)", 24));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendVerificationEmail("a@example.test", "A", "http://evil.example/verify", 24));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void acceptsHttpLocalhostForMailpitDevelopment() {
        service.sendVerificationEmail("a@example.test", "A",
                "http://localhost:3000/verify-email#token=test-token", 24);
        verify(mailSender).send(message);
    }

    private static String findContent(Part part, String type) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = findContent(multipart.getBodyPart(i), type);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        return part.isMimeType(type) && content instanceof String text ? text : null;
    }
}
