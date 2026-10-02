package dev.toqash.travelplannerbackend.trip;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public record TripResponse(
        UUID id,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        long durationDays,
        BudgetLevel budgetLevel,
        Pace pace,
        List<Vibe> vibes,
        String mustSee,
        String specialRequirements,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1,
                trip.getBudgetLevel(),
                trip.getPace(),
                trip.getVibes(),
                trip.getMustSee(),
                trip.getSpecialRequirements(),
                trip.getVersion(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}