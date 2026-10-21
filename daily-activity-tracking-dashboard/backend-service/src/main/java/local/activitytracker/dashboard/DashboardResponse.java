package local.activitytracker.dashboard;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate from,
        LocalDate to,
        int filesScanned,
        int taskCount,
        int activeDays,
        long knownDurationMinutes,
        List<CategorySummary> categories,
        List<DailySummary> dailyBreakdown,
        List<ActivityItem> activities,
        List<String> warnings
) {
    public record CategorySummary(
            String category,
            int taskCount,
            int activeDays,
            long knownDurationMinutes
    ) {
    }

    public record DailySummary(
            LocalDate date,
            String day,
            int taskCount
    ) {
    }

    public record ActivityItem(
            LocalDate date,
            String day,
            String category,
            String description,
            Integer durationMinutes,
            String sourceFile
    ) {
    }
}
