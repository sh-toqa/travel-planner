package dev.toqash.travelplannerbackend.planner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DestinationTipsController {
    private final DestinationTipsService destinationTipsService;

    public DestinationTipsController(DestinationTipsService destinationTipsService) {
        this.destinationTipsService = destinationTipsService;
    }

    @GetMapping("/trips/{tripId}/tips")
    public String tips(@PathVariable UUID tripId) {
        return destinationTipsService.tipsForTrip(tripId);
    }
}