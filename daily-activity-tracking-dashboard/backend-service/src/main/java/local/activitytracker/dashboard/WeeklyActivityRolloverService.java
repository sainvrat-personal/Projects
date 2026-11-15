package local.activitytracker.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class WeeklyActivityRolloverService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyActivityRolloverService.class);
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern WEEKLY_FILE = Pattern.compile(
            "^Week-(\\d{2}\\.\\d{2}\\.\\d{4})-(\\d{2}\\.\\d{2}\\.\\d{4})-Activity\\.md$"
    );
    private static final String TEMPLATE = """
            # Weekly Activity Tracking - Home

            > Use this page to record what you accomplish each day at home.
            > Optional for dashboard totals: add `[duration: 30m]` and/or `[category: Household]` to an activity bullet.

            **Week of:** __WEEK_RANGE__  
            **Focus for the week:** 

            ---

            ## Monday

            ### Completed

            - 

            ## Tuesday

            ### Completed

            - 

            ## Wednesday

            ### Completed

            - 

            ## Thursday

            ### Completed

            - 

            ## Friday

            ### Completed

            - 

            ## Saturday

            ### Completed

            - 

            ## Sunday

            ### Completed

            - 

            ---

            ## Weekly Summary

            ### Highlights

            - 

            ### Carry forward

            - 

            ### Notes

            - 
            """;

    private final Path trackerDirectory;
    private final Path archiveDirectory;

    public WeeklyActivityRolloverService(
            @Value("${tracker.source-directory}") String trackerDirectory,
            @Value("${tracker.archive-directory}") String archiveDirectory
    ) {
        this.trackerDirectory = Path.of(trackerDirectory).toAbsolutePath().normalize();
        this.archiveDirectory = Path.of(archiveDirectory).toAbsolutePath().normalize();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCurrentWeek() throws IOException {
        processCurrentWeek();
    }

    @Scheduled(cron = "${tracker.rollover.cron:0 5 0 * * *}")
    public void processCurrentWeek() throws IOException {
        rollover(LocalDate.now());
    }

    synchronized void rollover(LocalDate today) throws IOException {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        Files.createDirectories(trackerDirectory);
        Files.createDirectories(archiveDirectory);

        Path currentFile = trackerDirectory.resolve(weeklyFileName(monday, sunday));
        createCurrentSheet(currentFile, monday, sunday);
        archivePreviousSheets(monday);
    }

    private void createCurrentSheet(Path currentFile, LocalDate monday, LocalDate sunday) throws IOException {
        String content = TEMPLATE.replace("__WEEK_RANGE__", formatActivityDate(monday)
                + " to " + formatActivityDate(sunday));
        try {
            Files.writeString(currentFile, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            log("INFO", "Created current weekly activity sheet: " + currentFile);
        } catch (FileAlreadyExistsException exception) {
            log("INFO", "Current weekly activity sheet already exists; preserved it: " + currentFile);
        }
    }

    private void archivePreviousSheets(LocalDate currentMonday) throws IOException {
        try (Stream<Path> files = Files.list(trackerDirectory)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList()) {
                Matcher matcher = WEEKLY_FILE.matcher(file.getFileName().toString());
                if (!matcher.matches()) {
                    continue;
                }

                LocalDate weekStart;
                LocalDate weekEnd;
                try {
                    weekStart = LocalDate.parse(matcher.group(1), FILE_DATE);
                    weekEnd = LocalDate.parse(matcher.group(2), FILE_DATE);
                } catch (DateTimeParseException exception) {
                    log("WARN", "Ignored weekly filename with invalid dates: " + file);
                    continue;
                }
                if (!weekStart.getDayOfWeek().equals(DayOfWeek.MONDAY)
                        || !weekEnd.equals(weekStart.plusDays(6))
                        || !weekStart.isBefore(currentMonday)) {
                    continue;
                }

                Path archivedFile = archiveDirectory.resolve(file.getFileName());
                if (Files.exists(archivedFile)) {
                    if (Files.mismatch(file, archivedFile) == -1) {
                        Files.delete(file);
                        log("INFO", "Removed duplicate weekly activity sheet already archived: " + file);
                        continue;
                    }
                    log("WARN", "Archive conflict; left both copies untouched. Active-folder copy: "
                            + file + "; archived copy: " + archivedFile);
                    continue;
                }
                Files.move(file, archivedFile);
                log("INFO", "Archived weekly activity sheet: " + archivedFile);
            }
        }
    }

    private void log(String level, String message) throws IOException {
        String line = "[" + java.time.LocalDateTime.now() + "] [" + level + "] " + message;
        Files.writeString(archiveDirectory.resolve("activity-tracker.log"), line + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if ("WARN".equals(level)) {
            LOGGER.warn(message);
        } else {
            LOGGER.info(message);
        }
    }

    private static String weeklyFileName(LocalDate monday, LocalDate sunday) {
        return "Week-" + FILE_DATE.format(monday) + "-" + FILE_DATE.format(sunday) + "-Activity.md";
    }

    private static String formatActivityDate(LocalDate date) {
        int day = date.getDayOfMonth();
        int lastTwoDigits = day % 100;
        String suffix = lastTwoDigits >= 11 && lastTwoDigits <= 13
                ? "th"
                : switch (day % 10) {
                    case 1 -> "st";
                    case 2 -> "nd";
                    case 3 -> "rd";
                    default -> "th";
                };
        String month = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return day + suffix + " " + month + " " + date.getYear();
    }
}
