package dev.toqash.travelplannerbackend.planner;

import com.google.genai.errors.ApiException;
import dev.toqash.travelplannerbackend.itinerary.Activity;
import dev.toqash.travelplannerbackend.itinerary.ActivitySource;
import dev.toqash.travelplannerbackend.itinerary.Itinerary;
import dev.toqash.travelplannerbackend.itinerary.ItineraryDay;
import dev.toqash.travelplannerbackend.itinerary.ItineraryResponse;
import dev.toqash.travelplannerbackend.itinerary.ItineraryService;
import dev.toqash.travelplannerbackend.trip.TripResponse;
import dev.toqash.travelplannerbackend.trip.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItineraryGenerator {
    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerator.class);

    private final ChatClient chatClient;
    private final TripService tripService;
    private final ItineraryService itineraryService;
    private final ItineraryValidator validator;
    private final Resource systemPrompt;
    private final Resource userPrompt;

    public ItineraryGenerator(ChatClient chatClient,
                              TripService tripService,
                              ItineraryService itineraryService,
                              ItineraryValidator validator,
                              @Value("classpath:prompts/itinerary-system.st") Resource systemPrompt,
                              @Value("classpath:prompts/itinerary-user.st") Resource userPrompt) {
        this.chatClient = chatClient;
        this.tripService = tripService;
        this.itineraryService = itineraryService;
        this.validator = validator;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    // Deliberately not @Transactional: the model calls take seconds and must not hold a database connection.
    public ItineraryResponse generate(UUID tripId) {
        TripResponse trip = tripService.get(tripId); // ownership check
        PlanningContext context = PlanningContext.from(trip);

        ItineraryDraft draft = requestDraft(context, "");
        ValidationResult result = validator.validate(draft, trip);

        // One repair attempt: send the problems back. More attempts rarely help and multiply cost and latency.
        if (result.hasErrors()) {
            log.warn("Itinerary draft for trip {} was invalid, retrying once: {}", tripId, result.errors());
            draft = requestDraft(context, corrections(result.errors()));
            result = validator.validate(draft, trip);
            if (result.hasErrors()) {
                log.warn("Repaired itinerary draft for trip {} was still invalid: {}", tripId, result.errors());
                throw new AiOutputInvalidException(result.errors());
            }
        }
        return itineraryService.replaceForTrip(tripId, toItinerary(tripId, draft, result.warnings()));
    }

    // Returns null when the reply could not be read as an ItineraryDraft, so the validator reports it as an error.
    private ItineraryDraft requestDraft(PlanningContext context, String corrections) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(user -> user.text(userPrompt).params(context.asTemplateParams(corrections)))
                    .call()
                    .entity(ItineraryDraft.class);
        } catch (RuntimeException e) {
            if (hasCause(e, ApiException.class)) {
                throw new AiUnavailableException(e); // Gemini errors: overload (503), rate limit (429), ...
            }
            if (hasCause(e, JacksonException.class)) {
                log.warn("Model reply was not valid itinerary JSON: {}", e.getMessage());
                return null;
            }
            throw e;
        }
    }

    private static String corrections(List<String> errors) {
        return "Your previous plan was rejected because of these problems:\n"
                + errors.stream().map(error -> "- " + error).collect(Collectors.joining("\n"))
                + "\nReturn a complete, corrected plan that fixes all of them.";
    }

    private static boolean hasCause(Throwable e, Class<? extends Throwable> type) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    // Only called with a validated draft, so dates and times are known to parse.
    private Itinerary toItinerary(UUID tripId, ItineraryDraft draft, List<String> warnings) {
        Itinerary itinerary = Itinerary.builder()
                .tripId(tripId)
                .summary(draft.summary())
                .warnings(warnings)
                .build();
        for (ItineraryDraft.DayDraft dayDraft : draft.days()) {
            ItineraryDay day = ItineraryDay.builder()
                    .dayNumber(dayDraft.dayNumber())
                    .date(LocalDate.parse(dayDraft.date()))
                    .theme(dayDraft.theme())
                    .build();
            List<ItineraryDraft.ActivityDraft> activities = dayDraft.activities();
            for (int position = 0; position < activities.size(); position++) {
                day.addActivity(toActivity(activities.get(position), position));
            }
            itinerary.addDay(day);
        }
        return itinerary;
    }

    private Activity toActivity(ItineraryDraft.ActivityDraft draft, int position) {
        return Activity.builder()
                .position(position)
                .title(draft.title())
                .description(draft.description())
                .category(draft.category())
                .costLevel(draft.costLevel())
                .startTime(LocalTime.parse(draft.startTime()))
                .endTime(LocalTime.parse(draft.endTime()))
                .placeName(draft.placeName())
                .source(ActivitySource.AI)
                .build();
    }
}