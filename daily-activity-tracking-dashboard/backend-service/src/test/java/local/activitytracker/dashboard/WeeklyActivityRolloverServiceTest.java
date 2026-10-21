package local.activitytracker.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyActivityRolloverServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void createsCurrentTemplateAndArchivesPreviousWeeks() throws Exception {
        Path tracker = Files.createDirectory(tempDirectory.resolve("tracker"));
        Path archive = Files.createDirectory(tempDirectory.resolve("archive"));
        Path previousWeek = tracker.resolve("Week-05.10.2026-11.10.2026-Activity.md");
        Files.writeString(previousWeek, "Recorded activity");
        WeeklyActivityRolloverService rollover = new WeeklyActivityRolloverService(
                tracker.toString(), archive.toString()
        );

        rollover.rollover(LocalDate.parse("2026-10-15"));

        Path currentWeek = tracker.resolve("Week-12.10.2026-18.10.2026-Activity.md");
        assertTrue(Files.exists(currentWeek));
        assertTrue(Files.readString(currentWeek).contains("12th Oct 2026 to 18th Oct 2026"));
        assertEquals("Recorded activity", Files.readString(
                archive.resolve("Week-05.10.2026-11.10.2026-Activity.md")
        ));
        assertFalse(Files.exists(previousWeek));
    }

    @Test
    void preservesCurrentSheetAndBothFilesOnArchiveConflict() throws Exception {
        Path tracker = Files.createDirectory(tempDirectory.resolve("tracker"));
        Path archive = Files.createDirectory(tempDirectory.resolve("archive"));
        Path previousWeek = tracker.resolve("Week-05.10.2026-11.10.2026-Activity.md");
        Path archivedWeek = archive.resolve(previousWeek.getFileName());
        Path currentWeek = tracker.resolve("Week-12.10.2026-18.10.2026-Activity.md");
        Files.writeString(previousWeek, "Active copy");
        Files.writeString(archivedWeek, "Archived copy");
        Files.writeString(currentWeek, "Keep these activities");
        WeeklyActivityRolloverService rollover = new WeeklyActivityRolloverService(
                tracker.toString(), archive.toString()
        );

        rollover.rollover(LocalDate.parse("2026-10-15"));

        assertEquals("Keep these activities", Files.readString(currentWeek));
        assertEquals("Active copy", Files.readString(previousWeek));
        assertEquals("Archived copy", Files.readString(archivedWeek));
        assertTrue(Files.readString(archive.resolve("activity-tracker.log"))
                .contains("Archive conflict"));
    }
}
