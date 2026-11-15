package local.activitytracker.finance;

import java.math.BigDecimal;
import java.util.List;

public record FinanceResponse(
        String month,
        String monthLabel,
        String currency,
        int documentsFound,
        int documentsWithReadableText,
        BigDecimal totalIncome,
        BigDecimal totalExpenditure,
        String totalExpenditureBasis,
        List<CategoryTotal> categories,
        List<Transaction> transactions,
        List<DocumentStatus> documents,
        List<String> warnings
) {
    public record MonthOption(String value, String label) {
    }

    public record CategoryTotal(String category, BigDecimal amount, int transactionCount) {
    }

    public record Transaction(
            String date,
            String description,
            String category,
            String type,
            BigDecimal amount,
            String source
    ) {
    }

    public record DocumentStatus(
            String name,
            int pages,
            String status,
            int transactionsFound
    ) {
    }
}
