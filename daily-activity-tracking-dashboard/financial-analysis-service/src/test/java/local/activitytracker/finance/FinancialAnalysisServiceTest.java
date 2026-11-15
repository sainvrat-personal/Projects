package local.activitytracker.finance;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FinancialAnalysisServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void listsMonthsAndExcludesTheDuplicatePaniDudhPdf() throws Exception {
        Path january = Files.createDirectories(tempDirectory.resolve("2026/January"));
        createPdf(january.resolve("January financial.pdf"), """
                January 2026 monthly financial management statement with transaction detail
                available below for review by the local financial analysis dashboard.
                """);
        createPdf(january.resolve("January Pani camper & Dudh Hisab.pdf"), "duplicate");
        createPdf(Files.createDirectories(tempDirectory.resolve("2025/Oct")).resolve("Oct-2025.pdf"),
                "October 2025 expenses");

        FinancialAnalysisService service = new FinancialAnalysisService(tempDirectory.toString());

        assertEquals(
                java.util.List.of("2026-01", "2025-10"),
                service.availableMonths().stream().map(FinanceResponse.MonthOption::value).toList()
        );
        FinanceResponse report = service.analyze(YearMonth.of(2026, 1));
        assertEquals(1, report.documentsFound());
        assertEquals("January financial.pdf", report.documents().get(0).name());
    }

    @Test
    void extractsDatedIncomeAndExpenseRowsAndLeavesUnknownTotalsUnavailable() throws Exception {
        Path january = Files.createDirectories(tempDirectory.resolve("2026/Jan"));
        createPdf(january.resolve("statement.pdf"), """
                Date Description Amount
                01/01/2026 Grocery Store Rs 1,200.00
                02/01/2026 Salary Income Rs 40,000.00
                This statement lists the transactions recorded for the month for review.
                """);

        FinanceResponse report = new FinancialAnalysisService(tempDirectory.toString())
                .analyze(YearMonth.of(2026, 1));

        assertEquals(2, report.transactions().size());
        assertEquals("1200.00", report.totalExpenditure().toPlainString());
        assertEquals("40000.00", report.totalIncome().toPlainString());
        assertEquals("Groceries", report.categories().get(0).category());
        assertNull(new FinancialAnalysisService(tempDirectory.toString())
                .analyze(YearMonth.of(2026, 2)).totalExpenditure());
    }

    @Test
    void ignoresWordsThatResembleDatesAndInfersTheYearFromTheSelectedMonth() throws Exception {
        Path january = Files.createDirectories(tempDirectory.resolve("2026/Jan"));
        createPdf(january.resolve("statement.pdf"), """
                Date Description Amount
                Paid 4 Milk delivery Rs 200.00
                Jan 2 Pharmacy Rs 165.00
                The statement lists transactions recorded for this month for local review.
                """);

        FinanceResponse report = new FinancialAnalysisService(tempDirectory.toString())
                .analyze(YearMonth.of(2026, 1));

        assertEquals(1, report.transactions().size());
        assertEquals("2026-01-02", report.transactions().get(0).date());
        assertEquals("165.00", report.transactions().get(0).amount().toPlainString());
    }

    @Test
    void prefersFinalUsedSummaryAndDoesNotCountItsDetailRowsAgain() throws Exception {
        Path august = Files.createDirectories(tempDirectory.resolve("2026/August"));
        createPdf(august.resolve("summary.pdf"), """
                Tracking start from 1st August
                Total => 37k + 20k + 8.5k
                Final Used : 65.5k
                Home : 30k
                Astha : 7k
                Self : 12k (buggu-sbi) + 3k (mine-sbi) + 5k (cash) : 20k
                Extra used : 8.5k
                Total spent : 10,000 - 774 - 770 : 8,456 ~ 8.5k Extra
                01 August Grocery Store Rs 5,000
                This statement lists the monthly expenditure and supporting detail for review.
                """);
        createPdf(august.resolve("other.pdf"), """
                Date Description Amount
                02 August Pharmacy Rs 1,000
                This statement lists the recorded transactions for the month for local review.
                """);

        FinanceResponse report = new FinancialAnalysisService(tempDirectory.toString())
                .analyze(YearMonth.of(2026, 8));

        assertEquals(0, new BigDecimal("66500").compareTo(report.totalExpenditure()));
        assertEquals("DOCUMENT_TOTALS_AND_ROWS", report.totalExpenditureBasis());
        assertEquals(2, report.transactions().size());
    }

    @Test
    void ignoresNonPdfAndUncategorizedFiles() throws Exception {
        Files.createDirectories(tempDirectory.resolve("2026/April"));
        Files.writeString(tempDirectory.resolve("2026/April/notes.txt"), "not a PDF");

        FinancialAnalysisService service = new FinancialAnalysisService(tempDirectory.toString());

        assertEquals(0, service.availableMonths().size());
        assertFalse(service.analyze(YearMonth.of(2026, 4)).documentsFound() > 0);
    }

    private static void createPdf(Path file, String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.setLeading(14);
                content.newLineAtOffset(50, 750);
                for (String line : text.split("\\R")) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            document.save(file.toFile());
        }
    }
}
