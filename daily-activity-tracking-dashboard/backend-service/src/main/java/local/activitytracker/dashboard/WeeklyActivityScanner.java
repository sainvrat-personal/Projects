package local.activitytracker.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class WeeklyActivityScanner {
    private static final Pattern WEEK_FILE = Pattern.compile(
            "^Week-(\\d{2}\\.\\d{2}\\.\\d{4})-(\\d{2}\\.\\d{2}\\.\\d{4})-Activity\\.md$"
    );
    private static final Pattern TASK_LINE = Pattern.compile(
            "^[-*+]\\s+(?:\\[[ xX]\\]\\s+)?(.+?)\\s*$"
    );
    private static final Pattern DURATION = Pattern.compile(
            "(?i)\\[duration:\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\]"
    );
    private static final Pattern CATEGORY = Pattern.compile(
            "(?i)\\[category:\\s*([^\\]]+)\\]"
    );
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter
            .ofPattern("dd.MM.uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final List<String> DAY_NAMES = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );

    private final Path sourceDirectory;
    private final List<Path> archiveDirectories;
    private final ActivityClassifier classifier;

    public WeeklyActivityScanner(
            @Value("${tracker.source-directory}") String sourceDirectory,
            @Value("${tracker.archive-directory:}") String archiveDirectory,
            @Value("${tracker.legacy-archive-directory:}") String legacyArchiveDirectory,
            ActivityClassifier classifier
    ) {
        this.sourceDirectory = Path.of(sourceDirectory).toAbsolutePath().normalize();
        this.archiveDirectories = new ArrayList<>();
        addOptionalDirectory(archiveDirectory);
        addOptionalDirectory(legacyArchiveDirectory);
        this.classifier = classifier;
    }

    public DashboardResponse scan(LocalDate requestedFrom, LocalDate requestedTo) {
        if (requestedFrom != null && requestedTo != null && requestedFrom.isAfter(requestedTo)) {
            throw new IllegalArgumentException("'from' date must be on or before 'to' date.");
        }

        List<String> warnings = new ArrayList<>();
        List<Path> directories = new ArrayList<>();
        directories.add(sourceDirectory);
        directories.addAll(archiveDirectories);

        Map<String, Path> filesByName = new LinkedHashMap<>();
        for (Path directory : directories.stream().distinct().toList()) {
            if (!Files.exists(directory)) {
                if (directory.equals(sourceDirectory)) {
                    throw new IllegalStateException("Tracker source directory does not exist: " + directory);
                }
                continue;
            }
            if (!Files.isDirectory(directory)) {
                throw new IllegalStateException("Tracker path is not a directory: " + directory);
            }

            try (Stream<Path> entries = Files.list(directory)) {
                entries.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("Week-"))
                        .filter(path -> path.getFileName().toString().endsWith("-Activity.md"))
                        .forEach(path -> selectFile(path, filesByName, warnings));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read tracker directory: " + directory, exception);
            }
        }

        List<ParsedFile> parsedFiles = new ArrayList<>();
        for (Path file : filesByName.values()) {
            ParsedFile parsed = parseFile(file, warnings);
            if (parsed != null) {
                parsedFiles.add(parsed);
            }
        }

        List<DashboardResponse.ActivityItem> activities = parsedFiles.stream()
                .flatMap(parsed -> parsed.activities().stream())
                .filter(item -> requestedFrom == null || !item.date().isBefore(requestedFrom))
                .filter(item -> requestedTo == null || !item.date().isAfter(requestedTo))
                .sorted(Comparator.comparing(DashboardResponse.ActivityItem::date)
                        .thenComparing(DashboardResponse.ActivityItem::category)
                        .thenComparing(DashboardResponse.ActivityItem::description))
                .toList();

        Map<ActivityCategory, List<DashboardResponse.ActivityItem>> byCategory =
                new EnumMap<>(ActivityCategory.class);
        for (DashboardResponse.ActivityItem item : activities) {
            ActivityCategory.fromLabel(item.category()).ifPresent(category ->
                    byCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(item));
        }

        List<DashboardResponse.CategorySummary> categorySummaries = byCategory.entrySet().stream()
                .map(entry -> new DashboardResponse.CategorySummary(
                        entry.getKey().label(),
                        entry.getValue().size(),
                        (int) entry.getValue().stream()
                                .map(DashboardResponse.ActivityItem::date)
                                .distinct()
                                .count(),
                        entry.getValue().stream()
                                .map(DashboardResponse.ActivityItem::durationMinutes)
                                .filter(minutes -> minutes != null)
                                .mapToLong(Integer::longValue)
                                .sum()
                ))
                .sorted(Comparator.comparing(DashboardResponse.CategorySummary::category))
                .toList();

        Map<LocalDate, Integer> dailyCounts = new HashMap<>();
        activities.forEach(item -> dailyCounts.merge(item.date(), 1, Integer::sum));
        List<DashboardResponse.DailySummary> dailySummaries = dailyCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DashboardResponse.DailySummary(
                        entry.getKey(),
                        entry.getKey().getDayOfWeek().getDisplayName(
                                java.time.format.TextStyle.FULL, Locale.ENGLISH
                        ),
                        entry.getValue()
                ))
                .toList();

        LocalDate from = requestedFrom != null
                ? requestedFrom
                : activities.stream().map(DashboardResponse.ActivityItem::date)
                        .min(LocalDate::compareTo).orElse(null);
        LocalDate to = requestedTo != null
                ? requestedTo
                : activities.stream().map(DashboardResponse.ActivityItem::date)
                        .max(LocalDate::compareTo).orElse(null);

        return new DashboardResponse(
                from,
                to,
                parsedFiles.size(),
                activities.size(),
                (int) activities.stream().map(DashboardResponse.ActivityItem::date).distinct().count(),
                activities.stream()
                        .map(DashboardResponse.ActivityItem::durationMinutes)
                        .filter(minutes -> minutes != null)
                        .mapToLong(Integer::longValue)
                        .sum(),
                categorySummaries,
                dailySummaries,
                activities,
                warnings.stream().distinct().toList()
        );
    }

    private void addOptionalDirectory(String directory) {
        if (directory != null && !directory.isBlank()) {
            archiveDirectories.add(Path.of(directory).toAbsolutePath().normalize());
        }
    }

    private void selectFile(Path candidate, Map<String, Path> filesByName, List<String> warnings) {
        String name = candidate.getFileName().toString();
        if (!WEEK_FILE.matcher(name).matches()) {
            warnings.add("Ignored a weekly activity file with an invalid date-range filename: " + name);
            return;
        }

        Path existing = filesByName.get(name);
        if (existing == null) {
            filesByName.put(name, candidate);
            return;
        }

        try {
            Path preferred = Files.getLastModifiedTime(candidate)
                    .compareTo(Files.getLastModifiedTime(existing)) > 0 ? candidate : existing;
            Path other = preferred.equals(candidate) ? existing : candidate;
            filesByName.put(name, preferred);
            warnings.add("Duplicate weekly filename found; using the most recently modified copy '"
                    + preferred + "' and ignoring '" + other + "'.");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not compare duplicate tracker files named " + name, exception);
        }
    }

    private ParsedFile parseFile(Path file, List<String> warnings) {
        Matcher matcher = WEEK_FILE.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            return null;
        }

        LocalDate monday;
        LocalDate sunday;
        try {
            monday = LocalDate.parse(matcher.group(1), FILE_DATE);
            sunday = LocalDate.parse(matcher.group(2), FILE_DATE);
        } catch (DateTimeParseException exception) {
            warnings.add("Ignored file with invalid dates in its name: " + file.getFileName());
            return null;
        }
        if (monday.getDayOfWeek() != DayOfWeek.MONDAY || !sunday.equals(monday.plusDays(6))) {
            warnings.add("Ignored file whose filename is not a Monday-Sunday week: " + file.getFileName());
            return null;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read activity file: " + file, exception);
        }

        List<DashboardResponse.ActivityItem> items = new ArrayList<>();
        int dayIndex = -1;
        boolean inCompletedSection = false;
        for (String line : lines) {
            if (line.startsWith("## ")) {
                String heading = line.substring(3).trim();
                dayIndex = dayIndex(heading);
                inCompletedSection = false;
                continue;
            }
            if (line.startsWith("### ")) {
                inCompletedSection = dayIndex >= 0
                        && line.substring(4).trim().equalsIgnoreCase("Completed");
                continue;
            }
            if (!inCompletedSection) {
                continue;
            }

            Matcher taskMatcher = TASK_LINE.matcher(line);
            if (!taskMatcher.matches()) {
                continue;
            }
            String rawDescription = taskMatcher.group(1).trim();
            if (rawDescription.isBlank()) {
                continue;
            }
            items.add(parseActivity(
                    monday.plusDays(dayIndex),
                    DAY_NAMES.get(dayIndex),
                    rawDescription,
                    file.getFileName().toString(),
                    warnings
            ));
        }
        return new ParsedFile(file, monday, sunday, items);
    }

    private DashboardResponse.ActivityItem parseActivity(
            LocalDate date,
            String day,
            String rawDescription,
            String sourceFile,
            List<String> warnings
    ) {
        Integer durationMinutes = null;
        Matcher durationMatcher = DURATION.matcher(rawDescription);
        if (durationMatcher.find()) {
            try {
                long hours = durationMatcher.group(1) == null ? 0 : Long.parseLong(durationMatcher.group(1));
                long minutes = durationMatcher.group(2) == null ? 0 : Long.parseLong(durationMatcher.group(2));
                long total = Math.addExact(Math.multiplyExact(hours, 60), minutes);
                if (total > 0 && total <= Integer.MAX_VALUE) {
                    durationMinutes = (int) total;
                } else {
                    warnings.add("Ignored invalid duration annotation in " + sourceFile + ".");
                }
            } catch (NumberFormatException | ArithmeticException exception) {
                warnings.add("Ignored invalid duration annotation in " + sourceFile + ".");
            }
        } else if (rawDescription.toLowerCase(Locale.ROOT).contains("[duration:")) {
            warnings.add("Could not parse duration annotation in " + sourceFile + ".");
        }

        ActivityCategory category = null;
        Matcher categoryMatcher = CATEGORY.matcher(rawDescription);
        if (categoryMatcher.find()) {
            category = ActivityCategory.fromLabel(categoryMatcher.group(1)).orElse(null);
            if (category == null) {
                warnings.add("Unknown category annotation in " + sourceFile
                        + "; inferred a category from its wording instead.");
            }
        }
        if (category == null) {
            category = classifier.classify(rawDescription);
        }

        String description = DURATION.matcher(rawDescription).replaceAll("").trim();
        description = CATEGORY.matcher(description).replaceAll("").trim();
        description = description.replaceAll("\\s{2,}", " ");
        return new DashboardResponse.ActivityItem(
                date, day, category.label(), description, durationMinutes, sourceFile
        );
    }

    private int dayIndex(String heading) {
        for (int index = 0; index < DAY_NAMES.size(); index++) {
            if (DAY_NAMES.get(index).equalsIgnoreCase(heading)) {
                return index;
            }
        }
        return -1;
    }

    private record ParsedFile(
            Path file,
            LocalDate monday,
            LocalDate sunday,
            List<DashboardResponse.ActivityItem> activities
    ) {
    }
}
