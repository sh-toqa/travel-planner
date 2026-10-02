package dev.toqash.travelplannerbackend.trip;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

public record TripRequest(
        @Size(max = 100) String title,
        @NotBlank @Size(max = 200) String destination,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate,
        BudgetLevel budgetLevel,
        Pace pace,
        @NotNull @Size(min = 1, max = 3) List<@NotNull Vibe> vibes,
        @Size(max = 1000) String mustSee,
        @Size(max = 500) String specialRequirements,
        @NotNull(groups = OnUpdate.class) Long version
) {
    public static final int MAX_TRIP_DAYS = 14;

    @AssertTrue(message = "must not be before startDate")
    public boolean isEndDateValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "trip must be at most " + MAX_TRIP_DAYS + " days")
    public boolean isDurationValid() {
        return startDate == null || endDate == null
                || ChronoUnit.DAYS.between(startDate, endDate) < MAX_TRIP_DAYS;
    }

    @AssertTrue(message = "must not contain duplicates")
    public boolean isVibesUnique() {
        return vibes == null || new HashSet<>(vibes).size() == vibes.size();
    }
}