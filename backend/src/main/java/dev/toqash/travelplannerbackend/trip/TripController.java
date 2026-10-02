package dev.toqash.travelplannerbackend.trip;

import dev.toqash.travelplannerbackend.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springdoc.core.annotations.ParameterObject;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
@Tag(name = "Trips", description = "The caller's trips. Other users' trips always return 404.")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @Operation(summary = "Create a trip", description = "Budget level defaults to BALANCED, pace to MODERATE, title to 'Trip to {destination}'.")
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
        TripResponse trip = tripService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(trip.id())
                .toUri();
        return ResponseEntity.created(location).body(trip);
    }

    @GetMapping
    @Operation(summary = "List the caller's trips", description = "Paged; default sort is startDate ascending, page size at most 50.")
    public PageResponse<TripResponse> list(
            @ParameterObject
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(tripService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a trip")
    public TripResponse get(@PathVariable UUID id) {
        return tripService.get(id);
    }

    // OnUpdate = all default rules + version is required.
    @PutMapping("/{id}")
    @Operation(summary = "Replace a trip", description = "Send the version from your last read; a stale version returns 409 VERSION_CONFLICT.")
    public TripResponse update(@PathVariable UUID id,
                               @Validated(OnUpdate.class) @RequestBody TripRequest request) {
        return tripService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a trip and its itinerary")
    public void delete(@PathVariable UUID id) {
        tripService.delete(id);
    }
}