package dev.toqash.travelplannerbackend.trip;

import dev.toqash.travelplannerbackend.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripRequest request) {
        TripResponse trip = tripService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(trip.id())
                .toUri();
        return ResponseEntity.created(location).body(trip);
    }

    @GetMapping
    public PageResponse<TripResponse> list(
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(tripService.list(pageable));
    }

    @GetMapping("/{id}")
    public TripResponse get(@PathVariable UUID id) {
        return tripService.get(id);
    }

    // OnUpdate = all default rules + version is required.
    @PutMapping("/{id}")
    public TripResponse update(@PathVariable UUID id,
                               @Validated(OnUpdate.class) @RequestBody TripRequest request) {
        return tripService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tripService.delete(id);
    }
}