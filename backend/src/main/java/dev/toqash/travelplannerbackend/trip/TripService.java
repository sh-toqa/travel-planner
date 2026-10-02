package dev.toqash.travelplannerbackend.trip;

import dev.toqash.travelplannerbackend.user.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TripService {
    private static final BudgetLevel DEFAULT_BUDGET_LEVEL = BudgetLevel.BALANCED;
    private static final Pace DEFAULT_PACE = Pace.MODERATE;

    private final TripRepository tripRepository;
    private final CurrentUser currentUser;

    public TripService(TripRepository tripRepository, CurrentUser currentUser) {
        this.tripRepository = tripRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public TripResponse create(TripRequest request) {
        Trip trip = new Trip();
        trip.setOwnerId(currentUser.id());
        applyRequest(trip, request);
        return TripResponse.from(tripRepository.saveAndFlush(trip));
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> list(Pageable pageable) {
        return tripRepository.findAllByOwnerId(currentUser.id(), pageable)
                .map(TripResponse::from);
    }

    @Transactional(readOnly = true)
    public TripResponse get(UUID id) {
        return TripResponse.from(findOwnedTrip(id));
    }

    @Transactional
    public TripResponse update(UUID id, TripRequest request) {
        Trip trip = findOwnedTrip(id);
        // The client must have seen the latest version; @Version then guards the window until the flush.
        if (!trip.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(Trip.class, id);
        }
        applyRequest(trip, request);
        // Flush so the response carries the incremented version and new updatedAt.
        return TripResponse.from(tripRepository.saveAndFlush(trip));
    }

    @Transactional
    public void delete(UUID id) {
        tripRepository.delete(findOwnedTrip(id));
    }

    private Trip findOwnedTrip(UUID id) {
        return tripRepository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(TripNotFoundException::new);
    }

    private void applyRequest(Trip trip, TripRequest request) {
        String destination = request.destination().trim();
        String title = trimToNull(request.title());

        trip.setDestination(destination);
        trip.setTitle(title != null ? title : "Trip to " + destination);
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setBudgetLevel(request.budgetLevel() != null ? request.budgetLevel() : DEFAULT_BUDGET_LEVEL);
        trip.setPace(request.pace() != null ? request.pace() : DEFAULT_PACE);
        trip.setVibes(request.vibes());
        trip.setMustSee(trimToNull(request.mustSee()));
        trip.setSpecialRequirements(trimToNull(request.specialRequirements()));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}