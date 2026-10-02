package dev.toqash.travelplannerbackend.planner;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import dev.toqash.travelplannerbackend.itinerary.ActivityCategory;
import dev.toqash.travelplannerbackend.itinerary.CostLevel;

import java.util.List;

// The shape the model must return. Spring AI turns these records into a JSON schema for the prompt.
public record ItineraryDraft(
        @JsonPropertyDescription("One or two sentences describing the whole trip")
        String summary,
        @JsonPropertyDescription("Exactly one entry per trip day, in order")
        List<DayDraft> days
) {
    public record DayDraft(
            @JsonPropertyDescription("1 for the first day of the trip, 2 for the second, and so on")
            int dayNumber,
            @JsonPropertyDescription("The calendar date of this day, format YYYY-MM-DD")
            String date,
            @JsonPropertyDescription("Short theme for the day, e.g. 'Old town and street food'")
            String theme,
            @JsonPropertyDescription("Activities in chronological order")
            List<ActivityDraft> activities
    ) {
    }

    public record ActivityDraft(
            String title,
            @JsonPropertyDescription("One or two sentences on what to do and why it fits the traveller")
            String description,
            ActivityCategory category,
            CostLevel costLevel,
            @JsonPropertyDescription("24-hour local time, format HH:mm")
            String startTime,
            @JsonPropertyDescription("24-hour local time, format HH:mm, after startTime")
            String endTime,
            @JsonPropertyDescription("Name of the real place, venue or area, if there is one")
            String placeName
    ) {
    }
}