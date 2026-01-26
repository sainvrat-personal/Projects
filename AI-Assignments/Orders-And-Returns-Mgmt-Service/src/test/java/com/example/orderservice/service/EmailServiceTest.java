package com.example.orderservice.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Default configuration: sending enabled with a valid sender email.
        setField(emailService, "sendingEnabled", true);
        setField(emailService, "senderEmail", "from@example.com");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ---------------------------------------------------------------------
    // sendEmail tests
    // ---------------------------------------------------------------------

    @Test
    void sendEmail_whenSendingDisabled_logsAndDoesNotSend() throws Exception {
        setField(emailService, "sendingEnabled", false);

        emailService.sendEmail("to@example.com", "Subject", "Body");

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendEmail_whenRecipientEmpty_doesNotSend() {
        emailService.sendEmail("   ", "Subject", "Body");

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendEmail_whenSenderNotConfigured_logsAndDoesNotSend() throws Exception {
        setField(emailService, "senderEmail", "no-reply@example.com");

        emailService.sendEmail("to@example.com", "Subject", "Body");

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendEmail_success_sendsUsingJavaMailSender() {
        doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail("to@example.com", "Subject", "Body");

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_whenJavaMailSenderThrows_fallsBackToLogEmailAndDoesNotPropagate() {
        doThrow(new RuntimeException("SMTP failure"))
                .when(javaMailSender)
                .send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendEmail("to@example.com", "Subject", "Body")
        );

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    // ---------------------------------------------------------------------
    // sendInvoiceEmail tests
    // ---------------------------------------------------------------------

    @Test
    void sendInvoiceEmail_whenSendingDisabled_logsAndDoesNotSend() throws Exception {
        setField(emailService, "sendingEnabled", false);
        File attachment = File.createTempFile("invoice", ".pdf");

        emailService.sendInvoiceEmail("to@example.com", "Subject", "Body", attachment);

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendInvoiceEmail_whenRecipientEmpty_doesNotSend() throws Exception {
        File attachment = File.createTempFile("invoice", ".pdf");

        emailService.sendInvoiceEmail("   ", "Subject", "Body", attachment);

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendInvoiceEmail_whenAttachmentMissing_logsAndDoesNotSend() {
        emailService.sendInvoiceEmail("to@example.com", "Subject", "Body", null);

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendInvoiceEmail_whenSenderNotConfigured_logsAndDoesNotSend() throws Exception {
        setField(emailService, "senderEmail", "no-reply@example.com");
        File attachment = File.createTempFile("invoice", ".pdf");

        emailService.sendInvoiceEmail("to@example.com", "Subject", "Body", attachment);

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendInvoiceEmail_success_sendsMimeMessageWithAttachment() throws Exception {
        File attachment = File.createTempFile("invoice", ".pdf");

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        // Use real MimeMessage to allow MimeMessageHelper to operate normally.
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendInvoiceEmail("to@example.com", "Subject", "Body", attachment);

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendInvoiceEmail_whenAuthError_throwsRuntimeExceptionWithAuthMessage() throws Exception {
        File attachment = File.createTempFile("invoice", ".pdf");

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Authentication failed: bad credentials"))
                .when(javaMailSender)
                .send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> emailService.sendInvoiceEmail("to@example.com", "Subject", "Body", attachment)
        );

        assertEquals("Email authentication error while sending invoice email", ex.getMessage());
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Authentication failed"));
    }

    @Test
    void sendInvoiceEmail_whenNonAuthSmtpError_throwsRuntimeExceptionWithSmtpMessage() throws Exception {
        File attachment = File.createTempFile("invoice", ".pdf");

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Network unreachable"))
                .when(javaMailSender)
                .send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> emailService.sendInvoiceEmail("to@example.com", "Subject", "Body", attachment)
        );

        assertEquals("SMTP error while sending invoice email", ex.getMessage());
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Network unreachable"));
    }
}

