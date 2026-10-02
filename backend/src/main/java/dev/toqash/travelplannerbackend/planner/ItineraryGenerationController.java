package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.itinerary.ItineraryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
public class ItineraryGenerationController {
    private final ItineraryGenerator itineraryGenerator;

    public ItineraryGenerationController(ItineraryGenerator itineraryGenerator) {
        this.itineraryGenerator = itineraryGenerator;
    }

    // Synchronous for now: the request waits for the model.
    @PostMapping("/trips/{tripId}/itinerary/generate")
    public ResponseEntity<ItineraryResponse> generate(@PathVariable UUID tripId) {
        ItineraryResponse itinerary = itineraryGenerator.generate(tripId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/trips/{tripId}/itinerary")
                .buildAndExpand(tripId)
                .toUri();
        return ResponseEntity.created(location).body(itinerary);
    }
}