package dev.toqash.travelplannerbackend.planner;

import com.google.genai.errors.ServerException;
import dev.toqash.travelplannerbackend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItineraryGenerationIntegrationTest extends IntegrationTest {
    private static final LocalDate START = LocalDate.now().plusDays(30);

    @Test
    void generatesItineraryInTheBackground() throws Exception {
        String owner = registerUser();
        String tripId = createTwoDayTrip(owner);
        fakeChatModel.reply(plan(START.plusDays(1)));

        String jobUrl = startGeneration(owner, tripId);

        awaitJobStatus(owner, jobUrl, "SUCCEEDED");
        mockMvc.perform(get("/trips/" + tripId + "/itinerary").with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(2))
                .andExpect(jsonPath("$.days[0].activities[0].title").value("Tsukiji Market"))
                .andExpect(jsonPath("$.days[0].activities[0].source").value("AI"));
        // The prompt carries the trip's facts, and user text stays inside the <preferences> fence.
        String userPrompt = fakeChatModel.prompts().getFirst().getUserMessage().getText();
        assertThat(userPrompt).contains("Plan a trip to Tokyo lasting 2 days", "Day 1: " + START);
    }

    @Test
    void repairsInvalidPlanOnce() throws Exception {
        String owner = registerUser();
        String tripId = createTwoDayTrip(owner);
        fakeChatModel.reply(plan(START.plusDays(5)));   // wrong date for day 2
        fakeChatModel.reply(plan(START.plusDays(1)));

        awaitJobStatus(owner, startGeneration(owner, tripId), "SUCCEEDED");

        assertThat(fakeChatModel.prompts()).hasSize(2);
        assertThat(fakeChatModel.prompts().get(1).getUserMessage().getText())
                .contains("Your previous plan was rejected", "Day 2: date must be " + START.plusDays(1));
    }

    @Test
    void failsJobWhenPlanCannotBeRepaired() throws Exception {
        String owner = registerUser();
        String tripId = createTwoDayTrip(owner);
        fakeChatModel.reply(plan(START.plusDays(5)));
        fakeChatModel.reply(plan(START.plusDays(5)));

        String jobUrl = startGeneration(owner, tripId);

        awaitJobStatus(owner, jobUrl, "FAILED");
        mockMvc.perform(get(jobUrl).with(as(owner)))
                .andExpect(jsonPath("$.errorCode").value("AI_OUTPUT_INVALID"))
                .andExpect(jsonPath("$.errorDetails[0]").value("Day 2: date must be " + START.plusDays(1) + " but was " + START.plusDays(5) + "."));
        mockMvc.perform(get("/trips/" + tripId + "/itinerary").with(as(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void failsJobWhenProviderIsUnavailable() throws Exception {
        String owner = registerUser();
        String tripId = createTwoDayTrip(owner);
        fakeChatModel.fail(new RuntimeException("Failed to generate content",
                new ServerException(503, "UNAVAILABLE", "This model is currently experiencing high demand.")));

        String jobUrl = startGeneration(owner, tripId);

        awaitJobStatus(owner, jobUrl, "FAILED");
        mockMvc.perform(get(jobUrl).with(as(owner)))
                .andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"));
    }

    @Test
    void allowsOnlyOneActiveGenerationPerTrip() throws Exception {
        String owner = registerUser();
        String tripId = createTwoDayTrip(owner);
        fakeChatModel.hold();
        fakeChatModel.reply(plan(START.plusDays(1)));

        String jobUrl = startGeneration(owner, tripId);
        mockMvc.perform(post("/trips/" + tripId + "/itinerary/generate").with(as(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GENERATION_IN_PROGRESS"));

        fakeChatModel.release();
        awaitJobStatus(owner, jobUrl, "SUCCEEDED");
    }

    @Test
    void hidesJobsAndTripsFromOtherUsers() throws Exception {
        String owner = registerUser();
        String stranger = registerUser();
        String tripId = createTwoDayTrip(owner);
        fakeChatModel.reply(plan(START.plusDays(1)));
        String jobUrl = startGeneration(owner, tripId);

        mockMvc.perform(get(jobUrl).with(as(stranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
        mockMvc.perform(post("/trips/" + tripId + "/itinerary/generate").with(as(stranger)))
                .andExpect(status().isNotFound());
        awaitJobStatus(owner, jobUrl, "SUCCEEDED");
    }

    private String createTwoDayTrip(String owner) throws Exception {
        String location = mockMvc.perform(post("/trips").with(as(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destination": "Tokyo", "startDate": "%s", "endDate": "%s", "pace": "RELAXED", "vibes": ["FOOD"]}
                                """.formatted(START, START.plusDays(1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String startGeneration(String owner, String tripId) throws Exception {
        MvcResult result = mockMvc.perform(post("/trips/" + tripId + "/itinerary/generate").with(as(owner)))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    // The worker runs on another thread, so poll the job like a client would.
    private void awaitJobStatus(String owner, String jobUrl, String expected) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mockMvc.perform(get(jobUrl).with(as(owner)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(expected)));
    }

    // A valid 2-day RELAXED plan; day 2's date is a parameter so tests can make it wrong.
    private static String plan(LocalDate dayTwoDate) {
        return """
                {"summary": "Food in Tokyo", "days": [
                  {"dayNumber": 1, "date": "%s", "theme": "Markets", "activities": [
                    {"title": "Tsukiji Market", "description": "Breakfast", "category": "FOOD", "costLevel": "LOW", "startTime": "08:00", "endTime": "10:00", "placeName": "Tsukiji Outer Market"},
                    {"title": "Senso-ji", "description": "Temple", "category": "CULTURE", "costLevel": "FREE", "startTime": "11:00", "endTime": "12:30", "placeName": "Senso-ji"}]},
                  {"dayNumber": 2, "date": "%s", "theme": "Food halls", "activities": [
                    {"title": "Depachika", "description": "Food hall", "category": "FOOD", "costLevel": "LOW", "startTime": "10:00", "endTime": "11:30", "placeName": "Isetan Shinjuku"},
                    {"title": "Ramen", "description": "Dinner", "category": "FOOD", "costLevel": "LOW", "startTime": "18:00", "endTime": "19:00", "placeName": "Ichiran Shibuya"}]}]}
                """.formatted(START, dayTwoDate);
    }
}