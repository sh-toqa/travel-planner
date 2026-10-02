package dev.toqash.travelplannerbackend.itinerary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
    Optional<Itinerary> findByTripId(UUID tripId);

    boolean existsByTripId(UUID tripId);
}