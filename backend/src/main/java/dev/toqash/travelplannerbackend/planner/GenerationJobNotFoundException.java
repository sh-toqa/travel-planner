package dev.toqash.travelplannerbackend.planner;

public class GenerationJobNotFoundException extends RuntimeException {
    public GenerationJobNotFoundException() {
        super("Generation job not found");
    }
}