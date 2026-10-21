package local.activitytracker.dashboard;

import java.util.Arrays;
import java.util.Optional;

public enum ActivityCategory {
    HOUSEHOLD("Household"),
    HEALTH("Health"),
    WORK_STUDY("Work / Study"),
    ERRANDS("Errands"),
    FAMILY_SOCIAL("Family / Social"),
    LEISURE("Leisure"),
    PERSONAL("Personal"),
    OTHER("Other");

    private final String label;

    ActivityCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Optional<ActivityCategory> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(category -> category.label.equalsIgnoreCase(label.trim())
                        || category.name().replace('_', ' ').equalsIgnoreCase(label.trim()))
                .findFirst();
    }
}
