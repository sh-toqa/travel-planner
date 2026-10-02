package dev.toqash.travelplannerbackend.planner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
    // Jobs are only visible to the user who requested them.
    Optional<GenerationJob> findByIdAndRequestedBy(UUID id, UUID requestedBy);

    Optional<GenerationJob> findFirstByTripIdAndStatusIn(UUID tripId, Collection<GenerationStatus> statuses);

    List<GenerationJob> findAllByStatusIn(Collection<GenerationStatus> statuses);
}