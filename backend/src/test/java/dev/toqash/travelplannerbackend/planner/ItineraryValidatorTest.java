package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.itinerary.ActivityCategory;
import dev.toqash.travelplannerbackend.itinerary.CostLevel;
import dev.toqash.travelplannerbackend.trip.BudgetLevel;
import dev.toqash.travelplannerbackend.trip.Pace;
import dev.toqash.travelplannerbackend.trip.TripResponse;
import dev.toqash.travelplannerbackend.trip.Vibe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryValidatorTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 1);

    private final ItineraryValidator validator = new ItineraryValidator();

    @Test
    void acceptsValidDraft() {
        ValidationResult result = validator.validate(twoDayDraft(), trip(BudgetLevel.BALANCED));

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void rejectsMissingDraft() {
        assertThat(validator.validate(null, trip(BudgetLevel.BALANCED)).errors())
                .containsExactly("The response did not contain any days.");
    }

    @Test
    void rejectsWrongNumberOfDays() {
        ItineraryDraft draft = new ItineraryDraft("summary", List.of(day(1, "2026-11-01", morning(), afternoon())));

        assertThat(validator.validate(draft, trip(BudgetLevel.BALANCED)).errors())
                .containsExactly("Expected exactly 2 days but got 1.");
    }

    @Test
    void rejectsWrongDayNumberAndDate() {
        ItineraryDraft draft = new ItineraryDraft("summary", List.of(
                day(1, "2026-11-01", morning(), afternoon()),
                day(3, "2026-11-05", morning(), afternoon())));

        assertThat(validator.validate(draft, trip(BudgetLevel.BALANCED)).errors()).containsExactly(
                "Day 2: dayNumber must be 2 but was 3.",
                "Day 2: date must be 2026-11-02 but was 2026-11-05.");
    }

    @Test
    void rejectsBadTimeFormat() {
        ItineraryDraft draft = draftWithFirstDay(activity("Museum", "9:00", "11:00", CostLevel.LOW), afternoon());

        assertThat(validator.validate(draft, trip(BudgetLevel.BALANCED)).errors())
                .containsExactly("Day 1, activity 1 (Museum): startTime must use HH:mm (24-hour) but was '9:00'.");
    }

    @Test
    void rejectsActivityEndingBeforeItStarts() {
        ItineraryDraft draft = draftWithFirstDay(activity("Museum", "11:00", "10:00", CostLevel.LOW), afternoon());

        assertThat(validator.validate(draft, trip(BudgetLevel.BALANCED)).errors())
                .containsExactly("Day 1, activity 1 (Museum): must end after it starts (11:00-10:00).");
    }

    @Test
    void rejectsOverlappingActivities() {
        ItineraryDraft draft = draftWithFirstDay(
                activity("Museum", "09:00", "12:00", CostLevel.LOW),
                activity("Lunch", "11:30", "13:00", CostLevel.LOW));

        assertThat(validator.validate(draft, trip(BudgetLevel.BALANCED)).errors())
                .containsExactly("Day 1, activity 2 (Lunch): starts at 11:30 before the previous activity ends at 12:00.");
    }

    @Test
    void warnsAboutCostAboveBudgetWithoutRejecting() {
        ItineraryDraft draft = draftWithFirstDay(activity("Sushi", "09:00", "10:00", CostLevel.HIGH), afternoon());

        ValidationResult result = validator.validate(draft, trip(BudgetLevel.BALANCED));

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).containsExactly("Day 1, activity 1 (Sushi): HIGH cost on a BALANCED budget.");
    }

    @Test
    void warnsAboutActivityCountOutsidePace() {
        ItineraryDraft draft = draftWithFirstDay(morning());

        assertThat(validator.validate(draft, trip(BudgetLevel.BALANCED)).warnings())
                .containsExactly("Day 1: has 1 activities; RELAXED pace suggests 2 to 3.");
    }

    // A 2-day RELAXED trip (2-3 activities per day).
    private static TripResponse trip(BudgetLevel budget) {
        return new TripResponse(UUID.randomUUID(), "Trip to Tokyo", "Tokyo", START, START.plusDays(1), 2,
                budget, Pace.RELAXED, List.of(Vibe.FOOD), null, null, 0L, null, null);
    }

    private static ItineraryDraft twoDayDraft() {
        return new ItineraryDraft("summary", List.of(
                day(1, "2026-11-01", morning(), afternoon()),
                day(2, "2026-11-02",
                        activity("Market", "09:00", "11:00", CostLevel.LOW),
                        activity("Garden", "13:00", "15:00", CostLevel.FREE))));
    }

    private static ItineraryDraft draftWithFirstDay(ItineraryDraft.ActivityDraft... activities) {
        return new ItineraryDraft("summary", List.of(
                day(1, "2026-11-01", activities),
                twoDayDraft().days().get(1)));
    }

    private static ItineraryDraft.DayDraft day(int number, String date, ItineraryDraft.ActivityDraft... activities) {
        return new ItineraryDraft.DayDraft(number, date, "theme", List.of(activities));
    }

    private static ItineraryDraft.ActivityDraft morning() {
        return activity("Temple", "09:00", "11:00", CostLevel.FREE);
    }

    private static ItineraryDraft.ActivityDraft afternoon() {
        return activity("Ramen", "12:00", "13:00", CostLevel.LOW);
    }

    private static ItineraryDraft.ActivityDraft activity(String title, String start, String end, CostLevel cost) {
        return new ItineraryDraft.ActivityDraft(title, "description", ActivityCategory.CULTURE, cost, start, end, title);
    }
}