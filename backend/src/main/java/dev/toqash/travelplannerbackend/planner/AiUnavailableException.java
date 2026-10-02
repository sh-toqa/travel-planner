package dev.toqash.travelplannerbackend.planner;

// The AI provider failed (overloaded, rate-limited, unreachable)
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(Throwable cause) {
        super("The itinerary service is temporarily unavailable. Please try again shortly.", cause);
    }
}