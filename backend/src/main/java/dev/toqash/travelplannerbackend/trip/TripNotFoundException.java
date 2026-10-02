package dev.toqash.travelplannerbackend.trip;

public class TripNotFoundException extends RuntimeException {
    public TripNotFoundException() {
        super("Trip not found");
    }
}