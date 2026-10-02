package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.config.ErrorCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GenerationJobResponse(
        UUID id,
        UUID tripId,
        GenerationStatus status,
        ErrorCode errorCode,
        String errorMessage,
        List<String> errorDetails,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
    public static GenerationJobResponse from(GenerationJob job) {
        return new GenerationJobResponse(
                job.getId(),
                job.getTripId(),
                job.getStatus(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getErrorDetails(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }
}