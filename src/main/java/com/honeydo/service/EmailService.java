package com.honeydo.service;

import com.honeydo.exception.MailConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                         @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String to, String subject, String body) {
        MailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("Cannot send email to {}: spring.mail.host is not configured in application-local.properties", to);
            throw new MailConfigurationException(
                    "SMTP is not configured: set spring.mail.host in application-local.properties");
        }
        if (!StringUtils.hasText(fromAddress)) {
            log.error("Cannot send email to {}: spring.mail.username is not configured in application-local.properties", to);
            throw new MailConfigurationException(
                    "SMTP is not configured: set spring.mail.username in application-local.properties");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send email to {}", to, e);
            throw e;
        }
    }
}
