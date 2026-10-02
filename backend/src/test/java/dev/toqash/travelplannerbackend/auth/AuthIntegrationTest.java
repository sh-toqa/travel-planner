package dev.toqash.travelplannerbackend.auth;

import dev.toqash.travelplannerbackend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registersUserWithNormalisedEmailAndNoPasswordInResponse() throws Exception {
        String email = "New-" + UUID.randomUUID() + "@Test.dev";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "displayName": "  Toqa  "}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.displayName").value("Toqa"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        String email = registerUser();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void rejectsInvalidRegistrationWithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void returnsCurrentUserOnlyWithValidCredentials() throws Exception {
        String email = registerUser();

        mockMvc.perform(get("/users/me").with(as(email.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(get("/users/me").with(httpBasic(email, "wrong-password")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}