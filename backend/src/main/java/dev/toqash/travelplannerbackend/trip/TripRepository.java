package dev.toqash.travelplannerbackend.trip;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    Optional<Trip> findByIdAndOwnerId(UUID id, UUID ownerId);

    Page<Trip> findAllByOwnerId(UUID ownerId, Pageable pageable);
}