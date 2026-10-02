package dev.toqash.travelplannerbackend.planner;

import java.util.List;

// Errors make a draft unusable; warnings are saved with the itinerary and shown to the user.
public record ValidationResult(List<String> errors, List<String> warnings) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}