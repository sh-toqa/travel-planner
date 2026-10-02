package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.itinerary.CostLevel;
import dev.toqash.travelplannerbackend.trip.BudgetLevel;
import dev.toqash.travelplannerbackend.trip.Pace;
import dev.toqash.travelplannerbackend.trip.TripResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// Checks a model-generated draft against the trip before anything is saved
@Component
public class ItineraryValidator {
    private static final Pattern TIME_FORMAT = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
    private static final LocalTime EARLIEST_START = LocalTime.of(7, 0);
    private static final LocalTime LATEST_END = LocalTime.of(23, 30);

    public ValidationResult validate(ItineraryDraft draft, TripResponse trip) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (draft == null || draft.days() == null || draft.days().isEmpty()) {
            errors.add("The response did not contain any days.");
            return new ValidationResult(errors, warnings);
        }
        if (draft.days().size() != trip.durationDays()) {
            errors.add("Expected exactly " + trip.durationDays() + " days but got " + draft.days().size() + ".");
        }

        Set<String> seenPlaces = new HashSet<>();
        for (int i = 0; i < draft.days().size(); i++) {
            validateDay(draft.days().get(i), i + 1, trip.startDate().plusDays(i), trip, seenPlaces, errors, warnings);
        }
        return new ValidationResult(errors, warnings);
    }

    private void validateDay(ItineraryDraft.DayDraft day, int expectedNumber, LocalDate expectedDate, TripResponse trip,
                             Set<String> seenPlaces, List<String> errors, List<String> warnings) {
        String label = "Day " + expectedNumber;
        if (day.dayNumber() != expectedNumber) {
            errors.add(label + ": dayNumber must be " + expectedNumber + " but was " + day.dayNumber() + ".");
        }
        if (!expectedDate.toString().equals(day.date())) {
            errors.add(label + ": date must be " + expectedDate + " but was " + day.date() + ".");
        }
        List<ItineraryDraft.ActivityDraft> activities = day.activities();
        if (activities == null || activities.isEmpty()) {
            errors.add(label + ": has no activities.");
            return;
        }

        int[] range = activityRange(trip.pace());
        if (activities.size() < range[0] || activities.size() > range[1]) {
            warnings.add(label + ": has " + activities.size() + " activities; " + trip.pace()
                    + " pace suggests " + range[0] + " to " + range[1] + ".");
        }

        LocalTime previousEnd = null;
        for (int a = 0; a < activities.size(); a++) {
            ItineraryDraft.ActivityDraft activity = activities.get(a);
            String name = label + ", activity " + (a + 1);
            if (activity.title() == null || activity.title().isBlank()) {
                errors.add(name + ": title is missing.");
            } else {
                name = name + " (" + activity.title() + ")";
            }
            if (activity.category() == null) {
                errors.add(name + ": category is missing.");
            }
            if (activity.costLevel() == null) {
                errors.add(name + ": costLevel is missing.");
            } else if (exceedsBudget(activity.costLevel(), trip.budgetLevel())) {
                warnings.add(name + ": " + activity.costLevel() + " cost on a " + trip.budgetLevel() + " budget.");
            }

            LocalTime start = parseTime(activity.startTime(), name + ": startTime", errors);
            LocalTime end = parseTime(activity.endTime(), name + ": endTime", errors);
            if (start != null && end != null) {
                if (!start.isBefore(end)) {
                    errors.add(name + ": must end after it starts (" + activity.startTime() + "-" + activity.endTime() + ").");
                }
                if (previousEnd != null && start.isBefore(previousEnd)) {
                    errors.add(name + ": starts at " + activity.startTime() + " before the previous activity ends at " + previousEnd + ".");
                }
                if (start.isBefore(EARLIEST_START) || end.isAfter(LATEST_END)) {
                    warnings.add(name + ": scheduled outside " + EARLIEST_START + "-" + LATEST_END + ".");
                }
                previousEnd = end;
            }

            String place = activity.placeName();
            if (place != null && !place.isBlank() && !seenPlaces.add(place.trim().toLowerCase(Locale.ROOT))) {
                warnings.add(name + ": " + place + " appears more than once in the trip.");
            }
        }
    }

    private static LocalTime parseTime(String value, String field, List<String> errors) {
        if (value == null || !TIME_FORMAT.matcher(value).matches()) {
            errors.add(field + " must use HH:mm (24-hour) but was '" + value + "'.");
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            errors.add(field + " is not a valid time: '" + value + "'.");
            return null;
        }
    }

    private static int[] activityRange(Pace pace) {
        return switch (pace) {
            case RELAXED -> new int[]{2, 3};
            case MODERATE -> new int[]{3, 4};
            case ACTIVE -> new int[]{5, 6};
        };
    }

    private static boolean exceedsBudget(CostLevel cost, BudgetLevel budget) {
        return switch (budget) {
            case BUDGET -> cost == CostLevel.MEDIUM || cost == CostLevel.HIGH;
            case BALANCED -> cost == CostLevel.HIGH;
            case PREMIUM -> false;
        };
    }
}