package dev.toqash.travelplannerbackend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Base class for tests that start the whole application: real HTTP handling, security, PostgreSQL and Flyway.
// Every subclass shares one Spring context and one database container, so only the first test pays the startup cost.
@SpringBootTest(properties = {
        "spring.ai.model.chat=none",      // switch off the real Gemini chat model
        "GEMINI_API_KEY=test-key",
        "DB_USERNAME=unused",             // the container's connection details replace these
        "DB_PASSWORD=unused"
})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegrationTest.FakeAiConfiguration.class})
public abstract class IntegrationTest {
    protected static final String PASSWORD = "supersecret123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected FakeChatModel fakeChatModel;

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAiConfiguration {
        @Bean
        FakeChatModel fakeChatModel() {
            return new FakeChatModel();
        }
    }

    @BeforeEach
    void resetFakeModel() {
        fakeChatModel.reset();
    }

    // Each test registers its own users with unique emails, so tests never depend on each other's data.
    protected String registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.dev";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return email;
    }

    protected static org.springframework.test.web.servlet.request.RequestPostProcessor as(String email) {
        return httpBasic(email, PASSWORD);
    }
}