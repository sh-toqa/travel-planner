package dev.toqash.travelplannerbackend.planner;

import java.util.UUID;

public class GenerationInProgressException extends RuntimeException {
    private final UUID activeJobId;

    public GenerationInProgressException(UUID activeJobId) {
        super("An itinerary is already being generated for this trip");
        this.activeJobId = activeJobId;
    }

    public UUID getActiveJobId() {
        return activeJobId;
    }
}