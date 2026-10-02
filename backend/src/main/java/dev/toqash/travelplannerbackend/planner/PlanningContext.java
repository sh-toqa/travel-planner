package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.trip.Pace;
import dev.toqash.travelplannerbackend.trip.TripResponse;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// What the model is told about the trip, and nothing else: no ids, no user data.
public record PlanningContext(
        String destination,
        long numberOfDays,
        String dayList,
        String budgetLevel,
        String pace,
        String activitiesPerDay,
        String primaryVibe,
        String vibes,
        String mustSee,
        String specialRequirements
) {
    public static PlanningContext from(TripResponse trip) {
        long days = trip.durationDays();
        return new PlanningContext(
                trip.destination(),
                days,
                IntStream.range(0, (int) days)
                        .mapToObj(i -> describeDay(i + 1, trip.startDate().plusDays(i)))
                        .collect(Collectors.joining("\n")),
                trip.budgetLevel().name(),
                trip.pace().name(),
                activitiesPerDay(trip.pace()),
                trip.vibes().getFirst().name(),
                trip.vibes().stream().map(Enum::name).collect(Collectors.joining(", ")),
                orNone(trip.mustSee()),
                orNone(trip.specialRequirements())
        );
    }

    // Keys match the {placeholders} in prompts/itinerary-user.st.
    public Map<String, Object> asTemplateParams() {
        return Map.of(
                "destination", destination,
                "numberOfDays", numberOfDays,
                "dayList", dayList,
                "budgetLevel", budgetLevel,
                "pace", pace,
                "activitiesPerDay", activitiesPerDay,
                "primaryVibe", primaryVibe,
                "vibes", vibes,
                "mustSee", mustSee,
                "specialRequirements", specialRequirements
        );
    }

    private static String describeDay(int number, LocalDate date) {
        return "Day " + number + ": " + date + " (" + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ")";
    }

    private static String activitiesPerDay(Pace pace) {
        return switch (pace) {
            case RELAXED -> "2 to 3";
            case MODERATE -> "3 to 4";
            case ACTIVE -> "5 to 6";
        };
    }

    private static String orNone(String value) {
        return value == null ? "none" : value;
    }
}