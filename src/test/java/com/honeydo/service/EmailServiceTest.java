package com.honeydo.service;

import com.honeydo.exception.MailConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);

    @Test
    void sendsEmailWhenSmtpIsConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        EmailService emailService = new EmailService(mailSenderProvider, "honeydo@example.com");

        emailService.sendEmail("user@example.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void throwsDescriptiveErrorWhenMailHostIsMissing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        EmailService emailService = new EmailService(mailSenderProvider, "honeydo@example.com");

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "Body"))
                .isInstanceOf(MailConfigurationException.class)
                .hasMessageContaining("spring.mail.host");
    }

    @Test
    void throwsDescriptiveErrorWhenFromAddressIsMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        EmailService emailService = new EmailService(mailSenderProvider, "");

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "Body"))
                .isInstanceOf(MailConfigurationException.class)
                .hasMessageContaining("spring.mail.username");
    }
}
