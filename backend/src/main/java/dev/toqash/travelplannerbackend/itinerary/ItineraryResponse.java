package dev.toqash.travelplannerbackend.itinerary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ItineraryResponse(
        UUID id,
        UUID tripId,
        String summary,
        ItineraryStatus status,
        List<String> warnings,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<DayResponse> days
) {
    public static ItineraryResponse from(Itinerary itinerary) {
        return new ItineraryResponse(
                itinerary.getId(),
                itinerary.getTripId(),
                itinerary.getSummary(),
                itinerary.getStatus(),
                itinerary.getWarnings(),
                itinerary.getVersion(),
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt(),
                itinerary.getDays().stream().map(DayResponse::from).toList()
        );
    }

    public record DayResponse(
            UUID id,
            int dayNumber,
            LocalDate date,
            String theme,
            String notes,
            List<ActivityResponse> activities
    ) {
        static DayResponse from(ItineraryDay day) {
            return new DayResponse(
                    day.getId(),
                    day.getDayNumber(),
                    day.getDate(),
                    day.getTheme(),
                    day.getNotes(),
                    day.getActivities().stream().map(ActivityResponse::from).toList()
            );
        }
    }

    public record ActivityResponse(
            UUID id,
            int position,
            String title,
            String description,
            ActivityCategory category,
            CostLevel costLevel,
            LocalTime startTime,
            LocalTime endTime,
            String placeName,
            Double latitude,
            Double longitude,
            boolean locked,
            ActivitySource source
    ) {
        static ActivityResponse from(Activity activity) {
            return new ActivityResponse(
                    activity.getId(),
                    activity.getPosition(),
                    activity.getTitle(),
                    activity.getDescription(),
                    activity.getCategory(),
                    activity.getCostLevel(),
                    activity.getStartTime(),
                    activity.getEndTime(),
                    activity.getPlaceName(),
                    activity.getLatitude(),
                    activity.getLongitude(),
                    activity.isLocked(),
                    activity.getSource()
            );
        }
    }
}