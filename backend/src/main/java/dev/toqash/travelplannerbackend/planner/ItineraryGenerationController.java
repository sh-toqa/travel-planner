package dev.toqash.travelplannerbackend.planner;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
public class ItineraryGenerationController {
    private final GenerationJobService jobService;

    public ItineraryGenerationController(GenerationJobService jobService) {
        this.jobService = jobService;
    }

    // 202 Accepted: the work has started; poll the Location until the job is SUCCEEDED or FAILED.
    @PostMapping("/trips/{tripId}/itinerary/generate")
    public ResponseEntity<GenerationJobResponse> generate(@PathVariable UUID tripId) {
        GenerationJobResponse job = jobService.start(tripId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/generation-jobs/{jobId}")
                .buildAndExpand(job.id())
                .toUri();
        return ResponseEntity.accepted().location(location).body(job);
    }

    @GetMapping("/generation-jobs/{jobId}")
    public GenerationJobResponse getJob(@PathVariable UUID jobId) {
        return jobService.get(jobId);
    }
}