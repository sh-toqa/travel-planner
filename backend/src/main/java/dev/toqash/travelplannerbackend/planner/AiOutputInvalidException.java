package dev.toqash.travelplannerbackend.planner;

import java.util.List;

public class AiOutputInvalidException extends RuntimeException {
    private final List<String> errors;

    public AiOutputInvalidException(List<String> errors) {
        super("Could not generate a valid itinerary. Please try again.");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}