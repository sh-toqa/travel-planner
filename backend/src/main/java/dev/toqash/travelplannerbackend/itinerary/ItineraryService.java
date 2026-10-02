package dev.toqash.travelplannerbackend.itinerary;

import dev.toqash.travelplannerbackend.trip.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ItineraryService {
    private final ItineraryRepository itineraryRepository;
    private final TripService tripService;

    public ItineraryService(ItineraryRepository itineraryRepository, TripService tripService) {
        this.itineraryRepository = itineraryRepository;
        this.tripService = tripService;
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getForTrip(UUID tripId) {
        tripService.get(tripId); // 404 if the trip doesn't exist or isn't the caller's
        return itineraryRepository.findByTripId(tripId)
                .map(ItineraryResponse::from)
                .orElseThrow(ItineraryNotFoundException::new);
    }

    // Replaces the trip's itinerary in one transaction: either the old one stays, or the new one is fully saved.
    @Transactional
    public ItineraryResponse replaceForTrip(UUID tripId, Itinerary newItinerary) {
        itineraryRepository.findByTripId(tripId).ifPresent(existing -> {
            itineraryRepository.delete(existing);
            // Flush the delete first: one itinerary per trip is a unique constraint.
            itineraryRepository.flush();
        });
        return ItineraryResponse.from(itineraryRepository.saveAndFlush(newItinerary));
    }
}