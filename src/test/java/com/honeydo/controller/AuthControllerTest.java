package com.honeydo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honeydo.dao.UserDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDAO userDAO;

    @Test
    void registerThenLoginReturnsJwt() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "auth-flow@example.com", "password", "password123"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registeringDuplicateEmailReturnsConflict() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "duplicate@example.com", "password", "password123"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String email = "wrong-pass@example.com";
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "password123"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "wrong-password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        String email = "bcrypt-check@example.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "password123"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String storedHash = userDAO.findByEmail(email).orElseThrow().getPasswordHash();

        assertThat(storedHash).matches("^\\$2[ab]\\$.*");
        assertThat(storedHash).isNotEqualTo("password123");
    }
}
