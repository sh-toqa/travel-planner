package dev.toqash.travelplannerbackend.planner;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Itinerary generation", description = "AI generation runs in the background as a job.")
public class ItineraryGenerationController {
    private final GenerationJobService jobService;

    public ItineraryGenerationController(GenerationJobService jobService) {
        this.jobService = jobService;
    }

    // 202 Accepted: the work has started; poll the Location until the job is SUCCEEDED or FAILED.
    @PostMapping("/trips/{tripId}/itinerary/generate")
    @Operation(summary = "Start generating an itinerary with AI",
            description = "Returns 202 with a job; poll the Location header until the job is SUCCEEDED or FAILED. "
                    + "Replaces the existing itinerary on success. 409 GENERATION_IN_PROGRESS if a job is already active for the trip.")
    public ResponseEntity<GenerationJobResponse> generate(@PathVariable UUID tripId) {
        GenerationJobResponse job = jobService.start(tripId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/generation-jobs/{jobId}")
                .buildAndExpand(job.id())
                .toUri();
        return ResponseEntity.accepted().location(location).body(job);
    }

    @GetMapping("/generation-jobs/{jobId}")
    @Operation(summary = "Get a generation job's status")
    public GenerationJobResponse getJob(@PathVariable UUID jobId) {
        return jobService.get(jobId);
    }
}