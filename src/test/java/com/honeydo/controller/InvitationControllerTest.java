package com.honeydo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String registerAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "password123"));

        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private Long getUserId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private Long getOwnedListId(String email) {
        Long ownerId = getUserId(email);
        return jdbcTemplate.queryForObject("SELECT id FROM lists WHERE owner_id = ?", Long.class, ownerId);
    }

    private void shareListWithUser(String ownerEmail, String collaboratorEmail) {
        Long ownerId = getUserId(ownerEmail);
        Long collaboratorId = getUserId(collaboratorEmail);
        Long listId = jdbcTemplate.queryForObject(
                "SELECT id FROM lists WHERE owner_id = ?", Long.class, ownerId);
        jdbcTemplate.update(
                "INSERT INTO user_list (user_id, list_id) VALUES (?, ?)", collaboratorId, listId);
    }

    private String insertInvitation(Long listId, Long invitedByUserId, String email, String expiresAtSqlExpr) {
        String token = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO invitations (list_id, invited_by_user_id, email, token, expires_at) " +
                        "VALUES (?, ?, ?, ?, " + expiresAtSqlExpr + ")",
                listId, invitedByUserId, email, token);
        return token;
    }

    @Test
    void sendingInvitationCreatesInvitationRowWithSevenDayExpiry() throws Exception {
        String ownerToken = registerAndGetToken("invite-owner@example.com");

        String body = objectMapper.writeValueAsString(Map.of("email", "invitee@example.com"));

        mockMvc.perform(post("/lists/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        Long listId = getOwnedListId("invite-owner@example.com");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT email, created_at, expires_at, accepted_at FROM invitations WHERE list_id = ? AND email = ?",
                listId, "invitee@example.com");

        assertThat(row.get("email")).isEqualTo("invitee@example.com");
        assertThat(row.get("accepted_at")).isNull();

        // Verify expires_at is roughly 7 days after created_at
        Long diffSeconds = jdbcTemplate.queryForObject(
                "SELECT CAST((julianday(expires_at) - julianday(created_at)) * 86400 AS INTEGER) " +
                        "FROM invitations WHERE list_id = ? AND email = ?",
                Long.class, listId, "invitee@example.com");
        assertThat(diffSeconds).isCloseTo(7L * 24 * 60 * 60, org.assertj.core.data.Offset.offset(60L));
    }

    @Test
    void invitingExistingCollaboratorReturnsConflict() throws Exception {
        String ownerToken = registerAndGetToken("dup-owner@example.com");
        registerAndGetToken("dup-collaborator@example.com");
        shareListWithUser("dup-owner@example.com", "dup-collaborator@example.com");

        String body = objectMapper.writeValueAsString(Map.of("email", "dup-collaborator@example.com"));

        mockMvc.perform(post("/lists/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content -> {
                    String response = content.getResponse().getContentAsString();
                    assertThat(response).contains("This user already has access to this list");
                });
    }

    @Test
    void canInviteMultipleDifferentEmails() throws Exception {
        String ownerToken = registerAndGetToken("multi-owner@example.com");

        mockMvc.perform(post("/lists/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "first@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/lists/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "second@example.com"))))
                .andExpect(status().isCreated());

        Long listId = getOwnedListId("multi-owner@example.com");
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM invitations WHERE list_id = ?", Integer.class, listId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void revokeRemovesCollaboratorAndTheyLoseAccessToTasks() throws Exception {
        String ownerToken = registerAndGetToken("revoke-owner@example.com");
        String collaboratorToken = registerAndGetToken("revoke-collaborator@example.com");
        shareListWithUser("revoke-owner@example.com", "revoke-collaborator@example.com");

        // Owner creates a task on the shared list
        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Shared task"))))
                .andExpect(status().isCreated());

        // Collaborator can see it before revocation
        String beforeRevoke = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(beforeRevoke)).hasSize(1);

        Long collaboratorId = getUserId("revoke-collaborator@example.com");

        mockMvc.perform(delete("/lists/collaborators/" + collaboratorId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        Long listId = getOwnedListId("revoke-owner@example.com");
        Integer membershipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_list WHERE user_id = ? AND list_id = ?",
                Integer.class, collaboratorId, listId);
        assertThat(membershipCount).isEqualTo(0);

        // Collaborator no longer sees the list's tasks
        String afterRevoke = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(afterRevoke)).isEmpty();
    }

    @Test
    void getInvitationInfoWorksWithoutAuth() throws Exception {
        String ownerToken = registerAndGetToken("info-owner@example.com");
        Long ownerId = getUserId("info-owner@example.com");
        Long listId = getOwnedListId("info-owner@example.com");

        String token = insertInvitation(listId, ownerId, "info-invitee@example.com", "datetime('now', '+7 days')");

        String response = mockMvc.perform(get("/invitations/" + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        assertThat(json.get("listId").asLong()).isEqualTo(listId);
        assertThat(json.get("invitedEmail").asText()).isEqualTo("info-invitee@example.com");
        assertThat(json.get("expired").asBoolean()).isFalse();
        assertThat(json.get("accepted").asBoolean()).isFalse();

        // Quiet unused variable warning
        assertThat(ownerToken).isNotBlank();
    }

    @Test
    void acceptingInvitationAddsExistingUserToListAndAllowsTaskAccess() throws Exception {
        String ownerToken = registerAndGetToken("accept-owner@example.com");
        String inviteeToken = registerAndGetToken("accept-invitee@example.com");

        Long ownerId = getUserId("accept-owner@example.com");
        Long listId = getOwnedListId("accept-owner@example.com");

        String token = insertInvitation(listId, ownerId, "accept-invitee@example.com", "datetime('now', '+7 days')");

        // Owner creates a task on the shared list
        String taskResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Owner task"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        String acceptResponse = mockMvc.perform(post("/invitations/" + token + "/accept")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var accepted = objectMapper.readTree(acceptResponse);
        assertThat(accepted.get("listId").asLong()).isEqualTo(listId);

        Long inviteeId = getUserId("accept-invitee@example.com");
        Integer membershipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_list WHERE user_id = ? AND list_id = ?",
                Integer.class, inviteeId, listId);
        assertThat(membershipCount).isEqualTo(1);

        // Invitee can now see and edit the owner's task
        String inviteeTasks = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(inviteeTasks)).hasSize(1);

        mockMvc.perform(put("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + inviteeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Edited by invitee"))))
                .andExpect(status().isOk());

        String acceptedAt = jdbcTemplate.queryForObject(
                "SELECT accepted_at FROM invitations WHERE token = ?", String.class, token);
        assertThat(acceptedAt).isNotNull();
    }

    @Test
    void acceptingExpiredInvitationReturnsErrorAndDoesNotAddUser() throws Exception {
        String ownerToken = registerAndGetToken("expired-owner@example.com");
        String inviteeToken = registerAndGetToken("expired-invitee@example.com");

        Long ownerId = getUserId("expired-owner@example.com");
        Long listId = getOwnedListId("expired-owner@example.com");

        String token = insertInvitation(listId, ownerId, "expired-invitee@example.com", "datetime('now', '-1 day')");

        mockMvc.perform(post("/invitations/" + token + "/accept")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isGone());

        Long inviteeId = getUserId("expired-invitee@example.com");
        Integer membershipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_list WHERE user_id = ? AND list_id = ?",
                Integer.class, inviteeId, listId);
        assertThat(membershipCount).isEqualTo(0);

        // Quiet unused variable warning
        assertThat(ownerToken).isNotBlank();
    }

    @Test
    void nonOwnerCannotInviteOrRevokeOnTheirOwnList() throws Exception {
        String ownerToken = registerAndGetToken("perm-owner@example.com");
        String collaboratorToken = registerAndGetToken("perm-collaborator@example.com");
        shareListWithUser("perm-owner@example.com", "perm-collaborator@example.com");

        // The collaborator is not the owner of the shared list - inviting via /lists/invitations
        // operates on the collaborator's own owned list, not the shared list, so the shared
        // list's collaborator set is unaffected by the collaborator's actions.
        mockMvc.perform(post("/lists/invitations")
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "someone-else@example.com"))))
                .andExpect(status().isCreated());

        Long sharedListId = getOwnedListId("perm-owner@example.com");
        Integer invitationsOnSharedList = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM invitations WHERE list_id = ? AND email = ?",
                Integer.class, sharedListId, "someone-else@example.com");
        assertThat(invitationsOnSharedList).isEqualTo(0);

        // Collaborator attempting to revoke the owner from the shared list only affects
        // the collaborator's own list, where the owner is not a member - no-op, owner keeps access.
        Long ownerId = getUserId("perm-owner@example.com");
        mockMvc.perform(delete("/lists/collaborators/" + ownerId)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isNoContent());

        Integer ownerStillMember = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_list WHERE user_id = ? AND list_id = ?",
                Integer.class, ownerId, sharedListId);
        assertThat(ownerStillMember).isEqualTo(1);
    }

    @Test
    void listCollaboratorsReturnsOwnerAndCollaborators() throws Exception {
        String ownerToken = registerAndGetToken("list-collab-owner@example.com");
        registerAndGetToken("list-collab-collaborator@example.com");
        shareListWithUser("list-collab-owner@example.com", "list-collab-collaborator@example.com");

        String response = mockMvc.perform(get("/lists/collaborators")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(response);
        assertThat(json).hasSize(2);

        boolean ownerFound = false;
        boolean collaboratorFound = false;
        for (var node : json) {
            if (node.get("email").asText().equals("list-collab-owner@example.com")) {
                assertThat(node.get("owner").asBoolean()).isTrue();
                ownerFound = true;
            }
            if (node.get("email").asText().equals("list-collab-collaborator@example.com")) {
                assertThat(node.get("owner").asBoolean()).isFalse();
                collaboratorFound = true;
            }
        }
        assertThat(ownerFound).isTrue();
        assertThat(collaboratorFound).isTrue();
    }
}
