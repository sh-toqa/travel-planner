package dev.toqash.travelplannerbackend.planner;

import dev.toqash.travelplannerbackend.trip.TripResponse;
import dev.toqash.travelplannerbackend.trip.TripService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DestinationTipsService {
    private final ChatClient chatClient;
    private final TripService tripService;
    private final Resource tipsPrompt;

    public DestinationTipsService(ChatClient chatClient,
                                  TripService tripService,
                                  @Value("classpath:prompts/destination-tips.st") Resource tipsPrompt) {
        this.chatClient = chatClient;
        this.tripService = tripService;
        this.tipsPrompt = tipsPrompt;
    }

    public String tipsForTrip(UUID tripId) {
        TripResponse trip = tripService.get(tripId); // ownership check
        return chatClient.prompt()
                .user(user -> user.text(tipsPrompt)
                        .param("destination", trip.destination())
                        .param("vibes", trip.vibes().toString()))
                .call()
                .content();
    }
}