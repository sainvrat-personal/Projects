package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.lowagie.text.DocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InvoiceJobServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InvoiceJobService invoiceJobService;

    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderId = UUID.randomUUID();
        order = createTestOrder(orderId);
    }

    private Order createTestOrder(UUID id) {
        Order o = new Order();
        o.setId(id);
        o.setTransactionId(UUID.randomUUID());
        o.setCustomerId(UUID.randomUUID());
        o.setShippingAddress("123 Test Street");
        o.setEmail("customer@example.com");
        o.setProductId(UUID.randomUUID());
        o.setQuantity(2);
        o.setPrice(10.0);
        return o;
    }

    @Test
    void generateInvoice_usesExplicitEmailAndSendsInvoice() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        String explicitEmail = "  user@example.com  ";

        invoiceJobService.generateInvoice(orderId, explicitEmail);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);

        verify(emailService).sendInvoiceEmail(
                toCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture(),
                fileCaptor.capture()
        );

        assertEquals("user@example.com", toCaptor.getValue(), "Explicit email should be trimmed and used");
        assertNotNull(subjectCaptor.getValue());
        assertNotNull(bodyCaptor.getValue());

        File attachment = fileCaptor.getValue();
        assertNotNull(attachment, "Invoice PDF file must be provided");
        assertFalse(!attachment.exists(), "Invoice PDF must exist when email is sent");
    }

    @Test
    void generateInvoice_fallsBackToOrderEmailWhenParameterMissing() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        invoiceJobService.generateInvoice(orderId, null);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);

        verify(emailService).sendInvoiceEmail(
                toCaptor.capture(),
                anyString(),
                anyString(),
                fileCaptor.capture()
        );

        assertEquals(order.getEmail(), toCaptor.getValue(), "Order email should be used when parameter is null");

        File attachment = fileCaptor.getValue();
        assertNotNull(attachment);
        assertFalse(!attachment.exists(), "Invoice PDF must exist when email is sent");
    }

    @Test
    void generateInvoice_skipsEmailWhenNoEmailAvailable() {
        order.setEmail(null);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        invoiceJobService.generateInvoice(orderId, null);

        verifyNoInteractions(emailService);
    }

    @Test
    void generateInvoice_skipsEmailWhenEmailInvalid() {
        order.setEmail("invalid-email");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        invoiceJobService.generateInvoice(orderId, null);

        verifyNoInteractions(emailService);
    }

    @Test
    void generateInvoice_throwsWhenOrderNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> invoiceJobService.generateInvoice(orderId, "user@example.com")
        );

        assertEquals("Order not found with ID: " + orderId, ex.getMessage());
    }

    @Test
    void generateInvoice_cleansUpAndWrapsExceptionWhenEmailFailsWithDocumentException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        final File[] capturedAttachment = new File[1];

        doAnswer(invocation -> {
            File attachment = invocation.getArgument(3, File.class);
            capturedAttachment[0] = attachment;
            assertNotNull(attachment);
            assertFalse(!attachment.exists(), "Attachment should exist before failure");
            throw new RuntimeException(
                    "SMTP error",
                    new DocumentException("PDF layout failed")
            );
        }).when(emailService).sendInvoiceEmail(anyString(), anyString(), anyString(), any(File.class));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> invoiceJobService.generateInvoice(orderId, "user@example.com")
        );

        assertEquals(
                "Failed to generate/send invoice for order " + orderId,
                ex.getMessage()
        );

        // After failure, the service should attempt to clean up the invoice files.
        assertNotNull(capturedAttachment[0]);
        assertFalse(capturedAttachment[0].exists(), "Attachment should be deleted during cleanup");
    }

    @Test
    void generateInvoice_cleansUpAndWrapsExceptionWhenEmailFailsWithGenericException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        final File[] capturedAttachment = new File[1];

        doAnswer(invocation -> {
            File attachment = invocation.getArgument(3, File.class);
            capturedAttachment[0] = attachment;
            assertNotNull(attachment);
            assertFalse(!attachment.exists(), "Attachment should exist before failure");
            throw new RuntimeException("Generic SMTP error");
        }).when(emailService).sendInvoiceEmail(anyString(), anyString(), anyString(), any(File.class));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> invoiceJobService.generateInvoice(orderId, "user@example.com")
        );

        assertEquals(
                "Failed to generate/send invoice for order " + orderId,
                ex.getMessage()
        );

        assertNotNull(capturedAttachment[0]);
        assertFalse(capturedAttachment[0].exists(), "Attachment should be deleted during cleanup");
    }

    @Test
    void generateInvoiceAsync_swallowsIllegalArgumentException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> invoiceJobService.generateInvoiceAsync(orderId, "user@example.com"));
    }

    @Test
    void generateInvoiceAsync_swallowsUnexpectedExceptions() {
        InvoiceJobService spyService = spy(invoiceJobService);

        doThrow(new RuntimeException("Unexpected failure"))
                .when(spyService)
                .generateInvoice(orderId, "user@example.com");

        assertDoesNotThrow(() -> spyService.generateInvoiceAsync(orderId, "user@example.com"));

        verify(spyService).generateInvoice(orderId, "user@example.com");
    }
}

