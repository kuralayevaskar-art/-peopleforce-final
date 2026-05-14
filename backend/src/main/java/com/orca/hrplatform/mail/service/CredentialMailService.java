package com.orca.hrplatform.mail.service;

import com.orca.hrplatform.mail.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CredentialMailService {
    private final JavaMailSender mailSender;
    private final MailProperties properties;

    public void sendCredentials(String personalEmail, String corporateLogin, String temporaryPassword) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getHost())) {
            throw new IllegalStateException("SMTP is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(personalEmail);
        message.setSubject("Данные для входа в корпоративную учетную запись");
        message.setText("""
                Здравствуйте!

                Ваша корпоративная учетная запись успешно создана.

                Ваш логин:
                %s

                Ваш временный пароль:
                %s

                При первом входе в систему необходимо обязательно сменить временный пароль.

                Не передавайте логин и пароль другим лицам.

                С уважением,
                Администрация DMUK
                """.formatted(corporateLogin, temporaryPassword));
        try {
            mailSender.send(message);
        } catch (MailAuthenticationException ex) {
            throw new IllegalStateException("SMTP authentication failed. Check MAIL_USERNAME/MAIL_PASSWORD and enable Authenticated SMTP for this mailbox in Microsoft 365.", ex);
        }
    }

    public void sendRegistrationRejected(String personalEmail, String fullName, String reason) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getHost())) {
            throw new IllegalStateException("SMTP is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(personalEmail);
        message.setSubject("Заявка на регистрацию отклонена");
        message.setText("""
                Здравствуйте%s!

                Ваша заявка на регистрацию отклонена.

                Причина:
                %s

                Если вы считаете, что это ошибка, обратитесь к администратору DMUK.

                С уважением,
                Администрация DMUK
                """.formatted(StringUtils.hasText(fullName) ? ", " + fullName : "", StringUtils.hasText(reason) ? reason : "Причина не указана"));
        try {
            mailSender.send(message);
        } catch (MailAuthenticationException ex) {
            throw new IllegalStateException("SMTP authentication failed. Check MAIL_USERNAME/MAIL_PASSWORD and enable Authenticated SMTP for this mailbox in Microsoft 365.", ex);
        }
    }
}
