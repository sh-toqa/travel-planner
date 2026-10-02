package dev.toqash.travelplannerbackend.itinerary;

public class ItineraryNotFoundException extends RuntimeException {
    public ItineraryNotFoundException() {
        super("Itinerary not found");
    }
}