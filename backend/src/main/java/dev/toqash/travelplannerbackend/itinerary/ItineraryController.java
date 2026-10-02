package dev.toqash.travelplannerbackend.itinerary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/trips/{tripId}/itinerary")
@Tag(name = "Itineraries")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @GetMapping
    @Operation(summary = "Get a trip's itinerary", description = "Days and activities in order, plus validation warnings. 404 ITINERARY_NOT_FOUND until one is generated.")
    public ItineraryResponse get(@PathVariable UUID tripId) {
        return itineraryService.getForTrip(tripId);
    }
}