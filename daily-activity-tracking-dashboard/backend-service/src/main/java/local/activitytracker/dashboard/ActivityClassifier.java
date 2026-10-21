package local.activitytracker.dashboard;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ActivityClassifier {
    private final Map<ActivityCategory, List<String>> keywords = new EnumMap<>(ActivityCategory.class);

    public ActivityClassifier() {
        keywords.put(ActivityCategory.HOUSEHOLD, List.of(
                "clean", "washed", "wash", "dishes", "kitchen", "laundry", "cooked",
                "cooking", "room", "house", "home", "utensils", "water camper", "cat's potti"
        ));
        keywords.put(ActivityCategory.HEALTH, List.of(
                "hospital", "doctor", "docter", "medicine", "medicines", "therapy",
                "therapist", "injection", "drip", "opd", "health", "clinic", "sick"
        ));
        keywords.put(ActivityCategory.WORK_STUDY, List.of(
                "work", "office", "study", "studied", "college", "project", "exam",
                "course", "class", "computer certificate", "resume", "job", "insurance-claim"
        ));
        keywords.put(ActivityCategory.ERRANDS, List.of(
                "bought", "buy ", "purchase", "shopping", "market", "store", "shop",
                "paid", "payment", "picked up", "dropped off", "travelled", "bus", "taxi"
        ));
        keywords.put(ActivityCategory.FAMILY_SOCIAL, List.of(
                "called", "call ", "mom", "mother", "father", "brother", "sister",
                "family", "friend", "visited", "visit ", "met ", "came & met", "session"
        ));
        keywords.put(ActivityCategory.LEISURE, List.of(
                "played", "game", "movie", "movies", "music", "slept", "sleep", "rest",
                "relax", "watch"
        ));
        keywords.put(ActivityCategory.PERSONAL, List.of(
                "woke up", "wakeup", "bathed", "bath", "shower", "thoughts", "organized",
                "organised", "arranged", "scanned", "documents", "insurance claim"
        ));
    }

    public ActivityCategory classify(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        ActivityCategory bestCategory = ActivityCategory.OTHER;
        int bestScore = 0;

        for (ActivityCategory category : ActivityCategory.values()) {
            List<String> categoryKeywords = keywords.get(category);
            if (categoryKeywords == null) {
                continue;
            }
            int score = (int) categoryKeywords.stream().filter(normalized::contains).count();
            if (score > bestScore) {
                bestCategory = category;
                bestScore = score;
            }
        }
        return bestCategory;
    }

    public List<String> keywordsFor(ActivityCategory category) {
        return keywords.getOrDefault(category, List.of());
    }
}
