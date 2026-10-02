package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.itinerary.Activity;
import dev.toqash.travelplannerbackend.itinerary.ActivitySource;
import dev.toqash.travelplannerbackend.itinerary.Itinerary;
import dev.toqash.travelplannerbackend.itinerary.ItineraryDay;
import dev.toqash.travelplannerbackend.itinerary.ItineraryResponse;
import dev.toqash.travelplannerbackend.itinerary.ItineraryService;
import dev.toqash.travelplannerbackend.trip.TripResponse;
import dev.toqash.travelplannerbackend.trip.TripService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ItineraryGenerator {
    private final ChatClient chatClient;
    private final TripService tripService;
    private final ItineraryService itineraryService;
    private final Resource systemPrompt;
    private final Resource userPrompt;

    public ItineraryGenerator(ChatClient chatClient,
                              TripService tripService,
                              ItineraryService itineraryService,
                              @Value("classpath:prompts/itinerary-system.st") Resource systemPrompt,
                              @Value("classpath:prompts/itinerary-user.st") Resource userPrompt) {
        this.chatClient = chatClient;
        this.tripService = tripService;
        this.itineraryService = itineraryService;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    public ItineraryResponse generate(UUID tripId) {
        TripResponse trip = tripService.get(tripId); // ownership check
        PlanningContext context = PlanningContext.from(trip);

        ItineraryDraft draft = chatClient.prompt()
                .system(systemPrompt)
                .user(user -> user.text(userPrompt).params(context.asTemplateParams()))
                .call()
                .entity(ItineraryDraft.class);

        return itineraryService.replaceForTrip(tripId, toItinerary(tripId, draft));
    }

    private Itinerary toItinerary(UUID tripId, ItineraryDraft draft) {
        Itinerary itinerary = Itinerary.builder()
                .tripId(tripId)
                .summary(draft.summary())
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