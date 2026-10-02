package dev.toqash.travelplannerbackend.itinerary;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
    // Loads the days in the same query; activities follow in one batched query (default_batch_fetch_size).
    @EntityGraph(attributePaths = "days")
    Optional<Itinerary> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);
}