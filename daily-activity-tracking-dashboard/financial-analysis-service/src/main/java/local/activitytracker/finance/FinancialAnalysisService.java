package local.activitytracker.finance;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class FinancialAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialAnalysisService.class);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
    private static final Pattern YEAR_FOLDER = Pattern.compile("^20\\d{2}$");
    private static final Pattern NUMERIC_MONTH = Pattern.compile("^(20\\d{2})[-_](0[1-9]|1[0-2])$");
    private static final Pattern FILE_MONTH = Pattern.compile(
            "(?i)(January|February|March|April|May|June|July|August|September|October|November|December|"
                    + "Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[\\s_-]+(20\\d{2})"
    );
    private static final String MONTH_NAME =
            "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?"
                    + "|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)";
    private static final Pattern DATE = Pattern.compile(
            "(?i)(?<!\\d)(?:\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?"
                    + "|\\d{1,2}\\s+" + MONTH_NAME + "(?:\\s+\\d{2,4})?"
                    + "|" + MONTH_NAME + "\\s+\\d{1,2}(?:,?\\s+\\d{2,4})?)(?!\\d)"
    );
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)(?:₹\\s*|Rs\\.?\\s*|INR\\s*)?\\(?-?\\d[\\d,]*(?:\\.\\d{1,2})?\\s*[kK]?\\)?"
    );
    private static final Pattern TOTAL_EXPENSE = Pattern.compile(
            "(?i)(?:grand\\s+|monthly\\s+)?total.{0,40}(?:expense|expenditure|spent)"
                    + "|(?:expense|expenditure|spent).{0,40}(?:grand\\s+|monthly\\s+)?total"
    );
    private static final Pattern FINAL_USED = Pattern.compile("(?i)\\bfinal\\s+used\\b");
    private static final Pattern TOTAL_INCOME = Pattern.compile(
            "(?i)(?:grand\\s+|monthly\\s+)?total.{0,40}(?:income|received|credit)"
                    + "|(?:income|received|credit).{0,40}(?:grand\\s+|monthly\\s+)?total"
    );
    private static final int MAX_PDF_BYTES = 30 * 1024 * 1024;
    private static final int MAX_PDF_PAGES = 100;
    private static final int MIN_TEXT_CHARACTERS = 80;

    private final Path documentsDirectory;

    public FinancialAnalysisService(
            @Value("${financial.documents-directory}") String documentsDirectory
    ) {
        this.documentsDirectory = Path.of(documentsDirectory).toAbsolutePath().normalize();
    }

    public List<FinanceResponse.MonthOption> availableMonths() throws IOException {
        return findDocuments().stream()
                .map(MonthlyDocument::month)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .map(month -> new FinanceResponse.MonthOption(month.toString(), MONTH_LABEL.format(month)))
                .toList();
    }

    public FinanceResponse analyze(YearMonth month) throws IOException {
        List<MonthlyDocument> documents = findDocuments().stream()
                .filter(document -> document.month().equals(month))
                .sorted(Comparator.comparing(document -> document.path().getFileName().toString()))
                .toList();

        List<FinanceResponse.DocumentStatus> documentStatuses = new ArrayList<>();
        List<FinanceResponse.Transaction> transactions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        BigDecimal reportedExpenses = BigDecimal.ZERO;
        BigDecimal reportedIncome = BigDecimal.ZERO;
        boolean hasReportedExpense = false;
        boolean hasReportedIncome = false;
        boolean hasExplicitExpense = false;
        boolean hasRowExpenseFallback = false;
        int readableDocuments = 0;

        for (MonthlyDocument document : documents) {
            try {
                DocumentAnalysis result = analyzeDocument(document);
                documentStatuses.add(new FinanceResponse.DocumentStatus(
                        document.path().getFileName().toString(),
                        result.pages(),
                        result.status(),
                        result.transactions().size()
                ));
                if (result.readable()) {
                    readableDocuments++;
                }
                transactions.addAll(result.transactions());
                BigDecimal rowExpenses = sumTransactions(result.transactions(), "EXPENSE");
                BigDecimal rowIncome = sumTransactions(result.transactions(), "INCOME");
                boolean hasRowExpense = hasTransactions(result.transactions(), "EXPENSE");
                boolean hasRowIncome = hasTransactions(result.transactions(), "INCOME");
                if (result.explicitExpense() != null) {
                    reportedExpenses = reportedExpenses.add(result.explicitExpense());
                    hasReportedExpense = true;
                    hasExplicitExpense = true;
                } else if (hasRowExpense) {
                    reportedExpenses = reportedExpenses.add(rowExpenses);
                    hasReportedExpense = true;
                    hasRowExpenseFallback = true;
                }
                if (result.explicitIncome() != null) {
                    reportedIncome = reportedIncome.add(result.explicitIncome());
                    hasReportedIncome = true;
                } else if (hasRowIncome) {
                    reportedIncome = reportedIncome.add(rowIncome);
                    hasReportedIncome = true;
                }
                warnings.addAll(result.warnings());
            } catch (IOException exception) {
                LOGGER.warn("Could not analyze financial PDF {}", document.path().getFileName(), exception);
                documentStatuses.add(new FinanceResponse.DocumentStatus(
                        document.path().getFileName().toString(), 0, "ERROR", 0
                ));
                warnings.add("Could not read " + document.path().getFileName()
                        + ". Check that it is a valid, unencrypted PDF.");
            }
        }

        Map<String, CategoryAccumulator> categoryTotals = new LinkedHashMap<>();
        boolean hasTransactionExpense = false;
        for (FinanceResponse.Transaction transaction : transactions) {
            if (!"INCOME".equals(transaction.type())) {
                hasTransactionExpense = true;
                CategoryAccumulator accumulator = categoryTotals.computeIfAbsent(
                        transaction.category(), ignored -> new CategoryAccumulator()
                );
                accumulator.amount = accumulator.amount.add(transaction.amount());
                accumulator.count++;
            }
        }

        List<FinanceResponse.CategoryTotal> categories = categoryTotals.entrySet().stream()
                .map(entry -> new FinanceResponse.CategoryTotal(
                        entry.getKey(), entry.getValue().amount, entry.getValue().count
                ))
                .sorted(Comparator.comparing(FinanceResponse.CategoryTotal::amount).reversed())
                .toList();

        if (documents.isEmpty()) {
            warnings.add("No eligible financial PDFs were found for this month.");
        } else if (transactions.isEmpty() && !hasExplicitExpense && readableDocuments > 0) {
            warnings.add("PDF text was readable, but no transaction rows or explicit totals were recognized. "
                    + "Review the source documents before relying on totals.");
        }

        BigDecimal totalExpense = hasReportedExpense ? reportedExpenses : null;
        BigDecimal totalIncome = hasReportedIncome ? reportedIncome : null;
        String expenditureBasis = hasExplicitExpense
                ? hasRowExpenseFallback ? "DOCUMENT_TOTALS_AND_ROWS" : "DOCUMENT_TOTALS"
                : hasTransactionExpense ? "TRANSACTION_ROWS" : "UNAVAILABLE";
        if (hasExplicitExpense && hasTransactionExpense) {
            warnings.add("Each document's stated expenditure total is used when available. "
                    + "Rows from documents without a stated total are used instead; detail rows from summary "
                    + "documents are not added again.");
        }

        return new FinanceResponse(
                month.toString(),
                MONTH_LABEL.format(month),
                "INR",
                documents.size(),
                readableDocuments,
                totalIncome,
                totalExpense,
                expenditureBasis,
                categories,
                List.copyOf(transactions),
                List.copyOf(documentStatuses),
                warnings.stream().distinct().toList()
        );
    }

    private List<MonthlyDocument> findDocuments() throws IOException {
        if (!Files.isDirectory(documentsDirectory)) {
            throw new IOException("Financial documents directory does not exist: " + documentsDirectory);
        }

        try (Stream<Path> paths = Files.walk(documentsDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isIncludedPdf)
                    .map(this::monthFromPath)
                    .flatMap(Optional::stream)
                    .toList();
        }
    }

    private boolean isIncludedPdf(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".pdf")
                && !fileName.contains("pani camper")
                && !fileName.contains("dudh hisab");
    }

    private Optional<MonthlyDocument> monthFromPath(Path file) {
        Path relative = documentsDirectory.relativize(file);
        Path parent = relative.getParent();
        if (parent != null) {
            Path yearFolder = parent.getParent();
            if (yearFolder != null && YEAR_FOLDER.matcher(yearFolder.toString()).matches()) {
                Optional<Integer> monthNumber = parseMonth(parent.getFileName().toString());
                if (monthNumber.isPresent()) {
                    return Optional.of(new MonthlyDocument(
                            file, YearMonth.of(Integer.parseInt(yearFolder.toString()), monthNumber.get())
                    ));
                }
            }
        }

        Matcher numericMonth = NUMERIC_MONTH.matcher(relative.toString().replace('\\', '/'));
        if (numericMonth.find()) {
            return Optional.of(new MonthlyDocument(
                    file, YearMonth.of(Integer.parseInt(numericMonth.group(1)),
                    Integer.parseInt(numericMonth.group(2)))
            ));
        }

        Matcher fileMonth = FILE_MONTH.matcher(file.getFileName().toString());
        if (fileMonth.find()) {
            Optional<Integer> monthNumber = parseMonth(fileMonth.group(1));
            if (monthNumber.isPresent()) {
                return Optional.of(new MonthlyDocument(
                        file, YearMonth.of(Integer.parseInt(fileMonth.group(2)), monthNumber.get())
                ));
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> parseMonth(String folderName) {
        String normalized = folderName.toLowerCase(Locale.ROOT);
        for (int month = 1; month <= 12; month++) {
            String fullName = java.time.Month.of(month).getDisplayName(
                    java.time.format.TextStyle.FULL, Locale.ENGLISH
            ).toLowerCase(Locale.ROOT);
            if (fullName.startsWith(normalized) || normalized.startsWith(fullName.substring(0, 3))) {
                return Optional.of(month);
            }
        }
        return Optional.empty();
    }

    private DocumentAnalysis analyzeDocument(MonthlyDocument monthlyDocument) throws IOException {
        Path file = monthlyDocument.path();
        if (Files.size(file) > MAX_PDF_BYTES) {
            throw new IOException("PDF exceeds the configured size limit.");
        }

        try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
            int pages = pdf.getNumberOfPages();
            if (pages > MAX_PDF_PAGES) {
                throw new IOException("PDF exceeds the configured page limit.");
            }

            String text = new PDFTextStripper().getText(pdf);
            String status = "TEXT_EXTRACTED";
            if (text.replaceAll("\\s", "").length() < MIN_TEXT_CHARACTERS) {
                text = recognizeScannedPages(pdf);
                status = text.isBlank() ? "NO_READABLE_TEXT" : "OCR_EXTRACTED";
            }

            List<FinanceResponse.Transaction> transactions = parseTransactions(
                    text, file.getFileName().toString(), monthlyDocument.month()
            );
            BigDecimal explicitExpense = parseExplicitTotal(text, FINAL_USED);
            if (explicitExpense == null) {
                explicitExpense = parseExplicitTotal(text, TOTAL_EXPENSE);
            }
            BigDecimal explicitIncome = parseExplicitTotal(text, TOTAL_INCOME);
            List<String> warnings = new ArrayList<>();
            if ("NO_READABLE_TEXT".equals(status)) {
                warnings.add("No readable text could be extracted from " + file.getFileName()
                        + ". OCR may not recognize low-quality or handwritten content.");
            } else if ("OCR_EXTRACTED".equals(status)) {
                warnings.add("OCR may misread dates, descriptions, and amounts in " + file.getFileName()
                        + ". Verify recognized values against the source PDF.");
            }
            return new DocumentAnalysis(
                    pages, status, !text.isBlank(), transactions,
                    explicitExpense, explicitIncome, warnings
            );
        }
    }

    private static String recognizeScannedPages(PDDocument pdf) throws IOException {
        PDFRenderer renderer = new PDFRenderer(pdf);
        StringBuilder text = new StringBuilder();
        int pagesToRecognize = Math.min(pdf.getNumberOfPages(), 30);
        for (int page = 0; page < pagesToRecognize; page++) {
            Path imageFile = Files.createTempFile("activity-finance-ocr-", ".png");
            Path textFile = Files.createTempFile("activity-finance-ocr-", ".txt");
            try {
                BufferedImage image = renderer.renderImageWithDPI(page, 180);
                ImageIO.write(image, "png", imageFile.toFile());
                image.flush();
                Files.delete(textFile);
                Process process = new ProcessBuilder(
                        "tesseract", imageFile.toString(), textFile.toString().replaceFirst("\\.txt$", ""),
                        "--psm", "6", "-l", "eng"
                ).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                if (!process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new IOException("OCR process timed out.");
                }
                if (process.exitValue() == 0) {
                    text.append(Files.readString(textFile)).append('\n');
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("OCR was interrupted.", exception);
            } finally {
                Files.deleteIfExists(imageFile);
                Files.deleteIfExists(textFile);
            }
        }
        if (pdf.getNumberOfPages() > pagesToRecognize) {
            LOGGER.warn("OCR page limit reached for a financial PDF; remaining pages were not analyzed.");
        }
        return text.toString();
    }

    private static List<FinanceResponse.Transaction> parseTransactions(
            String text, String source, YearMonth month
    ) {
        List<FinanceResponse.Transaction> transactions = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String normalized = line.replace('|', ' ').replaceAll("\\s+", " ").trim();
            if (normalized.isEmpty() || normalized.length() > 500
                    || normalized.toLowerCase(Locale.ROOT).matches(".*\\b(total|subtotal|balance)\\b.*")) {
                continue;
            }

            Matcher dateMatcher = DATE.matcher(normalized);
            if (!dateMatcher.find()) {
                continue;
            }
            String date = dateMatcher.group();
            String withoutDate = normalized.substring(0, dateMatcher.start()) + " "
                    + normalized.substring(dateMatcher.end());
            Matcher amountMatcher = AMOUNT.matcher(withoutDate);
            int amountStart = -1;
            int amountEnd = -1;
            String rawAmount = null;
            while (amountMatcher.find()) {
                amountStart = amountMatcher.start();
                amountEnd = amountMatcher.end();
                rawAmount = amountMatcher.group();
            }
            if (rawAmount == null) {
                continue;
            }

            BigDecimal amount = parseAmount(rawAmount);
            if (amount == null || amount.signum() <= 0) {
                continue;
            }

            String description = (withoutDate.substring(0, amountStart) + " "
                    + withoutDate.substring(amountEnd))
                    .replaceAll("[^\\p{L}\\p{N}&'()./-]+", " ")
                    .replaceAll("\\s+", " ").trim();
            if (description.isBlank()) {
                description = "Unlabeled transaction";
            }
            String normalizedDescription = description.toLowerCase(Locale.ROOT);
            boolean income = containsAny(normalizedDescription,
                    "salary", "income", "credited", "credit", "deposit", "received", "refund");
            String type = income ? "INCOME" : "EXPENSE";
            String category = income ? "Income" : categorize(normalizedDescription);
            transactions.add(new FinanceResponse.Transaction(
                    normalizeDate(date, month), description, category, type, amount, source
            ));
        }
        return transactions;
    }

    private static BigDecimal sumTransactions(List<FinanceResponse.Transaction> transactions, String type) {
        return transactions.stream()
                .filter(transaction -> type.equals(transaction.type()))
                .map(FinanceResponse.Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean hasTransactions(List<FinanceResponse.Transaction> transactions, String type) {
        return transactions.stream().anyMatch(transaction -> type.equals(transaction.type()));
    }

    private static String normalizeDate(String dateText, YearMonth month) {
        String normalized = dateText.replaceAll(",", "").trim();
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ROOT),
                DateTimeFormatter.ofPattern("d-M-uuuu", Locale.ROOT),
                DateTimeFormatter.ofPattern("d/M/uu", Locale.ROOT),
                DateTimeFormatter.ofPattern("d-M-uu", Locale.ROOT),
                DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMMM d uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM d uuuu", Locale.ENGLISH),
                dateFormatterWithoutYear("d/M", month),
                dateFormatterWithoutYear("d-M", month),
                dateFormatterWithoutYear("d MMMM", month),
                dateFormatterWithoutYear("d MMM", month),
                dateFormatterWithoutYear("MMMM d", month),
                dateFormatterWithoutYear("MMM d", month)
        )) {
            try {
                return java.time.LocalDate.parse(normalized, formatter).toString();
            } catch (DateTimeParseException ignored) {
                // Try the next supported printed date format.
            }
        }
        return month + " " + normalized;
    }

    private static DateTimeFormatter dateFormatterWithoutYear(String pattern, YearMonth month) {
        return new DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.YEAR, month.getYear())
                .toFormatter(pattern.matches(".*[A-Za-z].*") ? Locale.ENGLISH : Locale.ROOT);
    }

    private static BigDecimal parseExplicitTotal(String text, Pattern totalPattern) {
        BigDecimal total = null;
        for (String line : text.split("\\R")) {
            if (!totalPattern.matcher(line).find()) {
                continue;
            }
            Matcher amountMatcher = AMOUNT.matcher(line);
            String rawAmount = null;
            while (amountMatcher.find()) {
                rawAmount = amountMatcher.group();
            }
            if (rawAmount != null) {
                total = parseAmount(rawAmount);
            }
        }
        return total;
    }

    private static BigDecimal parseAmount(String rawAmount) {
        String value = rawAmount.replaceAll("(?i)₹|Rs\\.?|INR|,", "").trim();
        boolean negative = value.startsWith("(") && value.endsWith(")");
        value = value.replace("(", "").replace(")", "");
        boolean thousands = value.toLowerCase(Locale.ROOT).endsWith("k");
        if (thousands) {
            value = value.substring(0, value.length() - 1).trim();
        }
        try {
            BigDecimal amount = new BigDecimal(value);
            amount = amount.abs();
            if (thousands) {
                amount = amount.multiply(BigDecimal.valueOf(1000));
            }
            return negative ? amount.negate() : amount;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String categorize(String description) {
        if (containsAny(description, "grocery", "vegetable", "fruit", "milk", "food", "ration", "supermarket")) {
            return "Groceries";
        }
        if (containsAny(description, "rent", "housing", "maintenance", "repair", "furniture")) {
            return "Housing";
        }
        if (containsAny(description, "electricity", "water bill", "gas bill", "internet", "mobile recharge",
                "utility", "telephone")) {
            return "Utilities";
        }
        if (containsAny(description, "hospital", "doctor", "medicine", "medical", "pharmacy", "clinic")) {
            return "Health";
        }
        if (containsAny(description, "fuel", "petrol", "diesel", "taxi", "cab", "bus", "train", "transport")) {
            return "Transport";
        }
        if (containsAny(description, "school", "college", "course", "education", "tuition")) {
            return "Education";
        }
        if (containsAny(description, "restaurant", "dining", "cafe", "food delivery", "swiggy", "zomato")) {
            return "Dining";
        }
        if (containsAny(description, "subscription", "netflix", "spotify", "membership")) {
            return "Subscriptions";
        }
        if (containsAny(description, "shopping", "clothes", "clothing", "amazon", "flipkart")) {
            return "Shopping";
        }
        return "Other";
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private record MonthlyDocument(Path path, YearMonth month) {
    }

    private record DocumentAnalysis(
            int pages,
            String status,
            boolean readable,
            List<FinanceResponse.Transaction> transactions,
            BigDecimal explicitExpense,
            BigDecimal explicitIncome,
            List<String> warnings
    ) {
    }

    private static final class CategoryAccumulator {
        private BigDecimal amount = BigDecimal.ZERO;
        private int count;
    }
}
