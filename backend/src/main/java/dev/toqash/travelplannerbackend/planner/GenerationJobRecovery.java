package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.config.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// Jobs live in an in-memory executor, so a restart loses them. Mark them failed instead of leaving them active forever.
@Component
public class GenerationJobRecovery {
    private static final Logger log = LoggerFactory.getLogger(GenerationJobRecovery.class);

    private final GenerationJobRepository jobRepository;

    public GenerationJobRecovery(GenerationJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failInterruptedJobs() {
        List<GenerationJob> interrupted = jobRepository.findAllByStatusIn(GenerationJobService.ACTIVE);
        for (GenerationJob job : interrupted) {
            job.setStatus(GenerationStatus.FAILED);
            job.setErrorCode(ErrorCode.INTERRUPTED);
            job.setErrorMessage("The server restarted before this generation finished. Please try again.");
            job.setFinishedAt(Instant.now());
        }
        if (!interrupted.isEmpty()) {
            log.warn("Marked {} interrupted generation job(s) as failed", interrupted.size());
        }
    }
}