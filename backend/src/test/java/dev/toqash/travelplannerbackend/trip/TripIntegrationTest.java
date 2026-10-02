package dev.toqash.travelplannerbackend.trip;

import dev.toqash.travelplannerbackend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TripIntegrationTest extends IntegrationTest {
    private static final LocalDate START = LocalDate.now().plusDays(30);

    @Test
    void createsTripWithDefaultsAndLocation() throws Exception {
        String owner = registerUser();

        mockMvc.perform(post("/trips").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Tokyo", START, START.plusDays(4), null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/trips/")))
                .andExpect(jsonPath("$.title").value("Trip to Tokyo"))
                .andExpect(jsonPath("$.budgetLevel").value("BALANCED"))
                .andExpect(jsonPath("$.pace").value("MODERATE"))
                .andExpect(jsonPath("$.durationDays").value(5))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void rejectsInvalidTrip() throws Exception {
        String owner = registerUser();

        mockMvc.perform(post("/trips").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Tokyo", START, START.plusDays(20), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("durationValid"));
    }

    @Test
    void listsOnlyTheCallersTrips() throws Exception {
        String alice = registerUser();
        String bob = registerUser();
        createTrip(alice, "Tokyo");
        createTrip(alice, "Rome");
        createTrip(bob, "Paris");

        mockMvc.perform(get("/trips").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void hidesTripsFromOtherUsers() throws Exception {
        String owner = registerUser();
        String stranger = registerUser();
        String location = createTrip(owner, "Tokyo");

        mockMvc.perform(get(location).with(as(stranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
        mockMvc.perform(put(location).with(as(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Hacked", START, START, 0L)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(location).with(as(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(location).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("Tokyo"));
    }

    @Test
    void rejectsUpdateWithStaleVersion() throws Exception {
        String owner = registerUser();
        String location = createTrip(owner, "Tokyo");

        mockMvc.perform(put(location).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Kyoto", START, START.plusDays(2), 0L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(put(location).with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson("Osaka", START, START.plusDays(2), 0L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void deletesTrip() throws Exception {
        String owner = registerUser();
        String location = createTrip(owner, "Tokyo");

        mockMvc.perform(delete(location).with(as(owner))).andExpect(status().isNoContent());
        mockMvc.perform(get(location).with(as(owner))).andExpect(status().isNotFound());
    }

    private String createTrip(String owner, String destination) throws Exception {
        return mockMvc.perform(post("/trips").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson(destination, START, START.plusDays(2), null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    void rejectsUnknownSortProperty() throws Exception {
        String owner = registerUser();

        mockMvc.perform(get("/trips").param("sort", "nonsense").with(as(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Unknown sort property: nonsense"));
    }

    private static String tripJson(String destination, LocalDate start, LocalDate end, Long version) {
        return """
                {"destination": "%s", "startDate": "%s", "endDate": "%s", "vibes": ["FOOD"], "version": %s}
                """.formatted(destination, start, end, version);
    }
}