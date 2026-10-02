package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.config.ErrorCode;
import dev.toqash.travelplannerbackend.trip.TripResponse;
import dev.toqash.travelplannerbackend.trip.TripService;
import dev.toqash.travelplannerbackend.user.CurrentUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GenerationJobService {
    static final Set<GenerationStatus> ACTIVE = EnumSet.of(GenerationStatus.PENDING, GenerationStatus.RUNNING);

    private final GenerationJobRepository jobRepository;
    private final TripService tripService;
    private final CurrentUser currentUser;
    private final GenerationWorker worker;
    private final TaskExecutor generationExecutor;

    public GenerationJobService(GenerationJobRepository jobRepository,
                                TripService tripService,
                                CurrentUser currentUser,
                                GenerationWorker worker,
                                @Qualifier("generationExecutor") TaskExecutor generationExecutor) {
        this.jobRepository = jobRepository;
        this.tripService = tripService;
        this.currentUser = currentUser;
        this.worker = worker;
        this.generationExecutor = generationExecutor;
    }

    // Not @Transactional: the job row must be committed before the worker thread looks for it.
    public GenerationJobResponse start(UUID tripId) {
        // Ownership is checked here, in the request thread. The worker has no logged-in user.
        TripResponse trip = tripService.get(tripId);

        GenerationJob job;
        try {
            job = jobRepository.saveAndFlush(GenerationJob.builder()
                    .tripId(tripId)
                    .requestedBy(currentUser.id())
                    .status(GenerationStatus.PENDING)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // The partial unique index refused a second active job for this trip.
            UUID activeJobId = jobRepository.findFirstByTripIdAndStatusIn(tripId, ACTIVE)
                    .map(GenerationJob::getId)
                    .orElse(null);
            throw new GenerationInProgressException(activeJobId);
        }

        UUID jobId = job.getId();
        try {
            generationExecutor.execute(() -> worker.run(jobId, trip));
        } catch (TaskRejectedException e) {
            markFailed(jobId, ErrorCode.AI_UNAVAILABLE, "Too many generations are running. Please try again shortly.", List.of());
            throw new AiUnavailableException(e);
        }
        return GenerationJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public GenerationJobResponse get(UUID jobId) {
        return jobRepository.findByIdAndRequestedBy(jobId, currentUser.id())
                .map(GenerationJobResponse::from)
                .orElseThrow(GenerationJobNotFoundException::new);
    }

    // State transitions: each one is its own short transaction, so progress is visible immediately.

    @Transactional
    public void markRunning(UUID jobId) {
        GenerationJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(GenerationStatus.RUNNING);
        job.setStartedAt(Instant.now());
    }

    @Transactional
    public void markSucceeded(UUID jobId) {
        GenerationJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(GenerationStatus.SUCCEEDED);
        job.setFinishedAt(Instant.now());
    }

    @Transactional
    public void markFailed(UUID jobId, ErrorCode code, String message, List<String> details) {
        GenerationJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(GenerationStatus.FAILED);
        job.setErrorCode(code);
        job.setErrorMessage(message);
        job.setErrorDetails(details);
        job.setFinishedAt(Instant.now());
    }
}