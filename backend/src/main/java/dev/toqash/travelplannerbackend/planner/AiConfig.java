package dev.toqash.travelplannerbackend.planner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // ChatClient.Builder is auto-configured by the Gemini starter; the app only depends on ChatClient.
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a concise, practical travel assistant.")
                .build();
    }
}