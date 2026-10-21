package local.activitytracker.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyActivityScannerTest {
    @TempDir
    Path tempDirectory;

    private final ActivityClassifier classifier = new ActivityClassifier();

    @Test
    void parsesTasksCategoriesDaysAndExplicitDurations() throws Exception {
        Path tracker = Files.createDirectory(tempDirectory.resolve("tracker"));
        Files.writeString(tracker.resolve("Week-05.10.2026-11.10.2026-Activity.md"), """
                # Weekly Activity Tracking - Home
                **Week of:** 5th Oct 2026 to 11th Oct 2026
                ## Monday
                ### Completed
                - Washed kitchen utensils [duration: 30m]
                - [category: Health] Visited hospital [duration: 1h 15m]
                  - nested detail is not a task
                ## Tuesday
                ### Completed
                - Played a game
                ## Weekly Summary
                ### Highlights
                - summary bullets are not daily tasks
                """, StandardCharsets.UTF_8);

        WeeklyActivityScanner scanner = new WeeklyActivityScanner(
                tracker.toString(), "", "", classifier
        );

        DashboardResponse response = scanner.scan(
                LocalDate.parse("2026-10-05"),
                LocalDate.parse("2026-10-11")
        );

        assertEquals(1, response.filesScanned());
        assertEquals(3, response.taskCount());
        assertEquals(2, response.activeDays());
        assertEquals(105, response.knownDurationMinutes());
        assertEquals(3, response.categories().size());
        assertEquals(3, response.dailyBreakdown().get(0).taskCount()
                + response.dailyBreakdown().get(1).taskCount());
        assertTrue(response.activities().stream()
                .anyMatch(item -> item.category().equals("Household") && item.durationMinutes() == 30));
        assertTrue(response.activities().stream()
                .anyMatch(item -> item.category().equals("Health") && item.durationMinutes() == 75));
    }

    @Test
    void reportsDuplicateNamesAndUsesTheMostRecentlyModifiedFile() throws Exception {
        Path tracker = Files.createDirectory(tempDirectory.resolve("tracker"));
        Path archive = Files.createDirectory(tempDirectory.resolve("archive"));
        String name = "Week-05.10.2026-11.10.2026-Activity.md";
        Files.writeString(tracker.resolve(name), """
                ## Monday
                ### Completed
                - Washed dishes
                """);
        Thread.sleep(20);
        Files.writeString(archive.resolve(name), """
                ## Tuesday
                ### Completed
                - Played a game
                """);

        WeeklyActivityScanner scanner = new WeeklyActivityScanner(
                tracker.toString(), archive.toString(), "", classifier
        );

        DashboardResponse response = scanner.scan(null, null);

        assertEquals(1, response.filesScanned());
        assertEquals(1, response.taskCount());
        assertEquals("Leisure", response.activities().get(0).category());
        assertTrue(response.warnings().stream().anyMatch(warning -> warning.contains("Duplicate weekly filename")));
    }

    @Test
    void rejectsInvertedDateRange() throws Exception {
        Path tracker = Files.createDirectory(tempDirectory.resolve("tracker"));
        WeeklyActivityScanner scanner = new WeeklyActivityScanner(
                tracker.toString(), "", "", classifier
        );

        assertThrows(IllegalArgumentException.class, () ->
                scanner.scan(LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-11")));
    }
}
