package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.config.ErrorCode;
import dev.toqash.travelplannerbackend.trip.TripResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// Runs on a background thread. No HTTP request to answer, so every outcome is recorded on the job instead.
@Component
public class GenerationWorker {
    private static final Logger log = LoggerFactory.getLogger(GenerationWorker.class);

    private final ItineraryGenerator generator;
    private final GenerationJobService jobService;

    // @Lazy breaks the constructor cycle: the job service starts the worker, the worker updates the job.
    public GenerationWorker(ItineraryGenerator generator, @Lazy GenerationJobService jobService) {
        this.generator = generator;
        this.jobService = jobService;
    }

    public void run(UUID jobId, TripResponse trip) {
        jobService.markRunning(jobId);
        try {
            generator.generate(trip);
            jobService.markSucceeded(jobId);
        } catch (AiUnavailableException e) {
            String cause = rootCauseMessage(e);
            log.warn("Generation job {}: AI provider unavailable: {}", jobId, cause);
            jobService.markFailed(jobId, ErrorCode.AI_UNAVAILABLE, e.getMessage(), List.of(cause));
        } catch (AiOutputInvalidException e) {
            jobService.markFailed(jobId, ErrorCode.AI_OUTPUT_INVALID, e.getMessage(), e.getErrors());
        } catch (RuntimeException e) {
            log.error("Generation job {} failed unexpectedly", jobId, e);
            jobService.markFailed(jobId, ErrorCode.INTERNAL_ERROR, "Unexpected error", List.of());
        }
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}