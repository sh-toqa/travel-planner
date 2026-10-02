package dev.toqash.travelplannerbackend.planner;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AiConfig {

    // ChatClient.Builder is auto-configured by the Gemini starter; the app only depends on ChatClient.
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a concise, practical travel assistant.")
                .build();
    }

    // At most 2 generations call the model at once (protects the free-tier rate limit); up to 20 more wait.
    // When the queue is full, new requests are rejected instead of piling up.
    @Bean
    public ThreadPoolTaskExecutor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("generation-");
        executor.initialize();
        return executor;
    }

    // Replaces the auto-configured Gemini client (it is @ConditionalOnMissingBean) to bound how long a call can take:
    // - a per-request timeout, so a hanging call fails instead of blocking a worker forever;
    // - no SDK-level retries: Spring AI already retries (spring.ai.retry.*), and two retry layers multiply the wait.
    @Bean
    public Client googleGenAiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey,
                                    @Value("${app.ai.request-timeout:90s}") java.time.Duration requestTimeout) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout((int) requestTimeout.toMillis())
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                        .build())
                .build();
    }
}