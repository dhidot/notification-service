package com.careertrack.notification.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
public final class SmtpVerificationEmailSender implements VerificationEmailSender {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");
    private static final String SUBJECT = "Aktivasi Akun CareerTrack";

    private final JavaMailSender mailSender;
    private final String senderAddress;

    public SmtpVerificationEmailSender(JavaMailSender mailSender,
                                       @Value("${careertrack.mail.from}") String senderAddress) {
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
    }

    @Override
    public void sendVerificationEmail(String recipientEmail, String recipientName,
                                      String verificationUrl, long expiresInHours) {
        requireBrowserSafeLink(verificationUrl);
        if (expiresInHours <= 0) {
            throw new IllegalArgumentException("Expiry must be positive");
        }

        String safeName = recipientName == null || recipientName.isBlank()
                ? "pengguna" : recipientName;
        String plainText = "Halo " + safeName + ",\n\n"
                + "Aktifkan akun CareerTrack melalui link berikut:\n"
                + verificationUrl + "\n\n"
                + "Link berlaku selama " + expiresInHours + " jam.\n"
                + "Jika kamu tidak mendaftar, abaikan email ini.";

        String htmlText = """
                <!doctype html>
                <html lang="id"><body style="font-family:Arial,sans-serif;line-height:1.6">
                <h2>Aktivasi akun CareerTrack</h2>
                <p>Halo %s,</p>
                <p>Klik tautan berikut untuk mengaktifkan akunmu:</p>
                <p><a href="%s">Verifikasi email</a></p>
                <p>Link berlaku selama %d jam. Jika kamu tidak mendaftar, abaikan email ini.</p>
                </body></html>
                """.formatted(HtmlUtils.htmlEscape(safeName),
                HtmlUtils.htmlEscape(verificationUrl), expiresInHours);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(senderAddress);
            helper.setTo(recipientEmail);
            helper.setSubject(SUBJECT);
            helper.setText(plainText, htmlText);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            // Do not log or propagate raw SMTP failures: they can contain message content.
            throw new EmailDeliveryException();
        }
    }

    private static void requireBrowserSafeLink(String verificationUrl) {
        if (verificationUrl == null) {
            throw new IllegalArgumentException("Invalid verification URL");
        }
        try {
            URI uri = URI.create(verificationUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean secure = scheme.equals("https");
            boolean localDev = scheme.equals("http") && LOCAL_HOSTS.contains(host);
            if (host.isEmpty() || uri.getRawUserInfo() != null || !(secure || localDev)) {
                throw new IllegalArgumentException("Invalid verification URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid verification URL");
        }
    }
}
