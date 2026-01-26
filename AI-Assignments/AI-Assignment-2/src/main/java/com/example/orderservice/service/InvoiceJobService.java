package com.example.orderservice.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import java.io.FileOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class InvoiceJobService {
    private final EmailService emailService;
    private final OrderRepository orderRepository;

    @Autowired
    public InvoiceJobService(EmailService emailService, OrderRepository orderRepository) {
        this.emailService = emailService;
        this.orderRepository = orderRepository;
    }

    /**
     * Asynchronous entry-point used by existing flows (e.g. order state
     * transition).
     * <p>
     * This method runs on the dedicated {@code invoiceJobExecutor} thread-pool
     * so that high-volume invoice generation cannot starve other async tasks.
     * It delegates to the synchronous implementation so that background job
     * processing can reuse the same logic and still surface failures.
     */
    @Async("invoiceJobExecutor")
    public void generateInvoiceAsync(UUID orderId, String email) {
        try {
            generateInvoice(orderId, email);
        } catch (IllegalArgumentException e) {
            // Common case: order no longer exists by the time the async worker
            // runs (e.g., data cleanup or cascade deletes). Log at WARN level
            // and treat as a no-op so that upstream flows are not surprised by
            // an unhandled async exception.
            log.warn("Skipping async invoice generation: {}", e.getMessage());
        } catch (Exception e) {
            // For unexpected failures in the ad-hoc async path, log a clear
            // error so operators can correlate issues even though no JobExecution
            // record is involved here.
            log.error("Async invoice generation failed for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Synchronous invoice generation used by the job scheduler.
     * Any failure in PDF creation or email sending will throw a RuntimeException
     * so that the job can be marked as FAILED/RETRY instead of silently succeeding.
     */
    public void generateInvoice(UUID orderId, String email) {
        log.info("[InvoiceJobService] Generating invoice for order: {}", orderId);

        // Fetch order details
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        // Derive the best-effort recipient email: prefer the explicit
        // parameter when present, otherwise fall back to the order's email.
        String targetEmail = (email != null && !email.trim().isEmpty())
                ? email.trim()
                : (order.getEmail() != null ? order.getEmail().trim() : null);

        // Create directory for invoices if it doesn't exist. Prefer the
        // standard container path, but gracefully fall back to a writable
        // temp-based directory if that path is not available (e.g. local dev).
        File directory = new File("/app/invoices");
        if (!directory.exists() && !directory.mkdirs()) {
            String tmpBase = System.getProperty("java.io.tmpdir", ".");
            File fallbackDir = new File(tmpBase, "invoices");
            if (!fallbackDir.exists() && !fallbackDir.mkdirs()) {
                throw new RuntimeException(
                        "Unable to create invoice directory at /app/invoices or fallback " + fallbackDir.getAbsolutePath());
            }
            log.warn("Primary invoice directory /app/invoices not available; using fallback {}", fallbackDir.getAbsolutePath());
            directory = fallbackDir;
        }

        // Generate PDF invoice with current date in filename. Write to a
        // temporary file first and then atomically move to the final
        // destination to avoid leaving behind truncated/locked files if an
        // error occurs mid-write.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        File finalFile = new File(directory, "invoice_" + orderId + "_" + timestamp + ".pdf");
        File tempFile;
        try {
            tempFile = File.createTempFile("invoice_" + orderId + "_" + timestamp + "_", ".pdf", directory);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create temporary file for invoice in " + directory.getAbsolutePath(), e);
        }
        String fileName = tempFile.getAbsolutePath();

        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Add title with larger, bold font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add invoice details
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // Invoice header with company info and invoice number
            Paragraph company = new Paragraph("Example Company Ltd.", boldFont);
            company.setAlignment(Element.ALIGN_RIGHT);
            document.add(company);

            Paragraph companyDetails = new Paragraph("123 Business Street\nCity, State 12345\nTax ID: 123456789",
                    normalFont);
            companyDetails.setAlignment(Element.ALIGN_RIGHT);
            companyDetails.setSpacingAfter(20);
            document.add(companyDetails);

            // Order and customer information
            document.add(new Paragraph("Invoice Number: INV-" + timestamp, boldFont));
            document.add(new Paragraph("Order ID: " + order.getId(), normalFont));
            document.add(new Paragraph("Transaction ID: " + order.getTransactionId(), normalFont));
            document.add(
                    new Paragraph("Date: " + new SimpleDateFormat("MMMM dd, yyyy").format(new Date()), normalFont));
            document.add(new Paragraph("Customer ID: " + order.getCustomerId(), normalFont));
            document.add(new Paragraph("Email: " + order.getEmail(), normalFont));
            document.add(new Paragraph("Shipping Address: " + order.getShippingAddress(), normalFont));
            document.add(new Paragraph("\n", normalFont));

            // Create a table for the order items
            PdfPTable table = new PdfPTable(4); // 4 columns
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Set column widths
            float[] columnWidths = { 1.5f, 1f, 1f, 1f };
            table.setWidths(columnWidths);

            // Add table headers
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            PdfPCell cell = new PdfPCell(new Phrase("Product", tableHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase("Quantity", tableHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase("Price", tableHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase("Total", tableHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);

            // Add order item details
            // For now, we only have one product per order
            cell = new PdfPCell(new Phrase("Product ID: " + order.getProductId(), normalFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setPadding(5);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(String.valueOf(order.getQuantity()), normalFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase("$" + String.format("%.2f", order.getPrice()), normalFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            table.addCell(cell);

            double totalAmount = order.getPrice() * order.getQuantity();
            cell = new PdfPCell(new Phrase("$" + String.format("%.2f", totalAmount), normalFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(5);
            table.addCell(cell);

            document.add(table);

            // Add totals section
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(40);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setSpacingBefore(10f);

            cell = new PdfPCell(new Phrase("Subtotal:", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBorder(0);
            cell.setPadding(5);
            totalsTable.addCell(cell);

            cell = new PdfPCell(new Phrase("$" + String.format("%.2f", totalAmount), normalFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setBorder(0);
            cell.setPadding(5);
            totalsTable.addCell(cell);

            // Calculate tax (e.g., 10%)
            double taxRate = 0.10;
            double taxAmount = totalAmount * taxRate;

            cell = new PdfPCell(new Phrase("Tax (10%):", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBorder(0);
            cell.setPadding(5);
            totalsTable.addCell(cell);

            cell = new PdfPCell(new Phrase("$" + String.format("%.2f", taxAmount), normalFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setBorder(0);
            cell.setPadding(5);
            totalsTable.addCell(cell);

            // Grand total
            double grandTotal = totalAmount + taxAmount;
            cell = new PdfPCell(new Phrase("Total:", boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBorder(0);
            cell.setPadding(5);
            totalsTable.addCell(cell);

            cell = new PdfPCell(new Phrase("$" + String.format("%.2f", grandTotal), boldFont));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setBorder(0);
            cell.setPadding(5);
            totalsTable.addCell(cell);

            document.add(totalsTable);

            // Add footer
            Paragraph thankYou = new Paragraph("\nThank you for your business!", boldFont);
            thankYou.setAlignment(Element.ALIGN_CENTER);
            thankYou.setSpacingBefore(20);
            document.add(thankYou);

            Paragraph termsAndConditions = new Paragraph(
                    "Payment is due within 30 days. Please make checks payable to Example Company Ltd.\n" +
                            "If you have any questions about this invoice, please contact customer service at support@example.com",
                    normalFont);
            termsAndConditions.setAlignment(Element.ALIGN_CENTER);
            termsAndConditions.setSpacingBefore(10);
            document.add(termsAndConditions);

            document.close();

            // Move the fully written temp file into place. If the move fails,
            // delete the temp file and surface an error so callers can decide
            // how to react.
            if (!tempFile.renameTo(finalFile)) {
                // Best-effort cleanup of temp artifact
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                throw new RuntimeException("Failed to move temporary invoice " + tempFile.getAbsolutePath()
                        + " to final location " + finalFile.getAbsolutePath());
            }
            log.info("PDF invoice generated: {}", finalFile.getAbsolutePath());

            // Send invoice email using EmailService, but treat missing/invalid
            // recipient addresses as a non-fatal condition: the invoice PDF is
            // still generated successfully, and we simply log instead of
            // attempting a doomed SMTP call.
            if (targetEmail == null || targetEmail.isEmpty()) {
                log.warn("Invoice generated for order {} at {} but no valid email is available; "
                        + "skipping invoice email send.", orderId, fileName);
            } else if (!targetEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                log.warn("Invoice generated for order {} at {} but email '{}' is invalid; "
                        + "skipping invoice email send.", orderId, fileName, targetEmail);
            } else {
                // Double-check that the final invoice file still exists before
                // attempting to attach it. This protects against rare cases
                // where the file was removed or moved by an external process
                // between generation and email sending.
                if (!finalFile.exists()) {
                    log.error("Expected invoice file {} for order {} is missing; "
                            + "skipping invoice email send.", finalFile.getAbsolutePath(), orderId);
                } else {
                    String subject = "Your Invoice for Order " + orderId;
                    String body = "Dear Customer,\n\n" +
                            "Thank you for your purchase! Your order has been processed successfully.\n\n" +
                            "Order ID: " + orderId + "\n" +
                            "Amount: $" + String.format("%.2f", grandTotal) + "\n\n" +
                            "Please find attached your invoice PDF.\n\n" +
                            "If you have any questions, please contact our customer service.\n\n" +
                            "Best regards,\n" +
                            "Example Company";

                    emailService.sendInvoiceEmail(targetEmail, subject, body, finalFile);
                }
            }
        } catch (Exception e) {
            // Best-effort cleanup for any partially written invoice artifacts.
            try {
                if (tempFile != null && tempFile.exists() && !tempFile.equals(finalFile)) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
                if (finalFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    finalFile.delete();
                }
            } catch (Exception cleanupEx) {
                log.warn("Error while cleaning up invoice files for order {}: {}", orderId, cleanupEx.getMessage(),
                        cleanupEx);
            }

            // Distinguish PDF/layout failures from other infrastructure errors
            // so logs are more actionable, while still surfacing a single
            // RuntimeException back to the caller (e.g., JobScheduler).
            Throwable root = e.getCause() != null ? e.getCause() : e;
            if (root instanceof com.lowagie.text.DocumentException) {
                log.error("PDF generation/layout failed for order {}: {}", orderId, root.getMessage(), e);
            } else {
                log.error("Failed to generate or send invoice for order {}: {}", orderId, e.getMessage(), e);
            }
            throw new RuntimeException("Failed to generate/send invoice for order " + orderId, e);
        }
    }
}
