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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

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

    private void shareListWithUser(String ownerEmail, String collaboratorEmail) {
        Long ownerId = getUserId(ownerEmail);
        Long collaboratorId = getUserId(collaboratorEmail);
        Long listId = jdbcTemplate.queryForObject(
                "SELECT id FROM lists WHERE owner_id = ?", Long.class, ownerId);
        jdbcTemplate.update(
                "INSERT INTO user_list (user_id, list_id) VALUES (?, ?)", collaboratorId, listId);
    }

    private void insertTaskIntoOwnList(Long userId, String title, boolean completed, String dueDate, Integer estimatedMinutes) {
        jdbcTemplate.update(
                "INSERT INTO tasks (user_id, title, completed, due_date, estimated_minutes) VALUES (?, ?, ?, ?, ?)",
                userId, title, completed ? 1 : 0, dueDate, estimatedMinutes);
        Long taskId = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        Long listId = jdbcTemplate.queryForObject(
                "SELECT id FROM lists WHERE owner_id = ?", Long.class, userId);
        jdbcTemplate.update("INSERT INTO task_list (task_id, list_id) VALUES (?, ?)", taskId, listId);
    }

    @Test
    void tasksAreOrderedWithIncompleteFirstThenByDueDateAscending() throws Exception {
        String email = "ordering@example.com";
        String token = registerAndGetToken(email);
        Long userId = getUserId(email);

        // Incomplete task with a later due date
        insertTaskIntoOwnList(userId, "Later incomplete task", false, "2026-12-31", 45);

        // Incomplete task with an earlier due date
        insertTaskIntoOwnList(userId, "Sooner incomplete task", false, "2026-01-01", 15);

        // Completed task with the earliest due date - should still appear last
        insertTaskIntoOwnList(userId, "Completed task", true, "2025-01-01", 30);

        String response = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tasks = objectMapper.readTree(response);

        assertThat(tasks).hasSize(3);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Sooner incomplete task");
        assertThat(tasks.get(1).get("title").asText()).isEqualTo("Later incomplete task");
        assertThat(tasks.get(2).get("title").asText()).isEqualTo("Completed task");
    }

    @Test
    void taskFieldsSerializeCorrectly() throws Exception {
        String email = "fields@example.com";
        String token = registerAndGetToken(email);
        Long userId = getUserId(email);

        insertTaskIntoOwnList(userId, "Wash the dishes", false, "2026-07-04", 20);

        String response = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tasks = objectMapper.readTree(response);

        assertThat(tasks).hasSize(1);
        var task = tasks.get(0);
        assertThat(task.get("title").asText()).isEqualTo("Wash the dishes");
        assertThat(task.get("completed").asBoolean()).isFalse();
        assertThat(task.get("dueDate").asText()).isEqualTo("2026-07-04");
        assertThat(task.get("estimatedMinutes").asInt()).isEqualTo(20);
    }

    @Test
    void emptyTaskListIsReturnedWhenUserHasNoTasks() throws Exception {
        String email = "empty@example.com";
        String token = registerAndGetToken(email);

        String response = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tasks = objectMapper.readTree(response);

        assertThat(tasks).isEmpty();
    }

    @Test
    void addingTaskWithTitleOnlyAppearsInList() throws Exception {
        String token = registerAndGetToken("add-title-only@example.com");

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Buy groceries"));

        String createResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var created = objectMapper.readTree(createResponse);
        assertThat(created.get("title").asText()).isEqualTo("Buy groceries");
        assertThat(created.get("completed").asBoolean()).isFalse();

        String listResponse = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tasks = objectMapper.readTree(listResponse);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Buy groceries");
    }

    @Test
    void addingTaskWithAllFieldsPersistsDueDateAndEstimatedMinutes() throws Exception {
        String token = registerAndGetToken("add-all-fields@example.com");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Mow the lawn", "dueDate", "2026-08-01", "estimatedMinutes", 60));

        String createResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var created = objectMapper.readTree(createResponse);
        assertThat(created.get("dueDate").asText()).isEqualTo("2026-08-01");
        assertThat(created.get("estimatedMinutes").asInt()).isEqualTo(60);
    }

    @Test
    void addingTaskWithBlankTitleIsRejected() throws Exception {
        String token = registerAndGetToken("add-blank-title@example.com");

        String createBody = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editingTaskUpdatesTitleDueDateAndEstimatedMinutes() throws Exception {
        String token = registerAndGetToken("edit-task@example.com");

        String createResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Original title"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "Updated title", "dueDate", "2026-09-01", "estimatedMinutes", 90));

        String updateResponse = mockMvc.perform(put("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var updated = objectMapper.readTree(updateResponse);
        assertThat(updated.get("title").asText()).isEqualTo("Updated title");
        assertThat(updated.get("dueDate").asText()).isEqualTo("2026-09-01");
        assertThat(updated.get("estimatedMinutes").asInt()).isEqualTo(90);
    }

    @Test
    void editingTaskDueDateReordersTaskInList() throws Exception {
        String token = registerAndGetToken("reorder-on-edit@example.com");

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task A", "dueDate", "2026-01-01"))))
                .andExpect(status().isCreated());

        String taskBResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task B", "dueDate", "2026-06-01"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskBId = objectMapper.readTree(taskBResponse).get("id").asLong();

        // Move Task B earlier than Task A
        mockMvc.perform(put("/tasks/" + taskBId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task B", "dueDate", "2025-12-01"))))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tasks = objectMapper.readTree(listResponse);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Task B");
        assertThat(tasks.get(1).get("title").asText()).isEqualTo("Task A");
    }

    @Test
    void deletingTaskRemovesItImmediatelyAndKeepsRemainingOrder() throws Exception {
        String token = registerAndGetToken("delete-task@example.com");

        String taskAResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task A", "dueDate", "2026-01-01"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskAId = objectMapper.readTree(taskAResponse).get("id").asLong();

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task B", "dueDate", "2026-02-01"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/tasks/" + taskAId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        String listResponse = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var tasks = objectMapper.readTree(listResponse);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("title").asText()).isEqualTo("Task B");
    }

    @Test
    void deletingLastTaskLeavesEmptyList() throws Exception {
        String token = registerAndGetToken("delete-last-task@example.com");

        String taskResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Only task"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        mockMvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        String listResponse = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(listResponse)).isEmpty();
    }

    @Test
    void markingTaskCompleteMovesItToBottomAndUncheckingRestoresOrder() throws Exception {
        String token = registerAndGetToken("mark-complete@example.com");

        String taskAResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task A", "dueDate", "2026-01-01"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskAId = objectMapper.readTree(taskAResponse).get("id").asLong();

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Task B", "dueDate", "2026-02-01"))))
                .andExpect(status().isCreated());

        // Mark Task A complete - should move to the bottom
        String completeResponse = mockMvc.perform(patch("/tasks/" + taskAId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("completed", true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(completeResponse).get("completed").asBoolean()).isTrue();

        String listAfterComplete = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var tasksAfterComplete = objectMapper.readTree(listAfterComplete);
        assertThat(tasksAfterComplete.get(0).get("title").asText()).isEqualTo("Task B");
        assertThat(tasksAfterComplete.get(1).get("title").asText()).isEqualTo("Task A");
        assertThat(tasksAfterComplete.get(1).get("completed").asBoolean()).isTrue();

        // Uncheck Task A - should be restored to incomplete and re-sorted by due date
        mockMvc.perform(patch("/tasks/" + taskAId + "/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("completed", false))))
                .andExpect(status().isOk());

        String listAfterUncheck = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var tasksAfterUncheck = objectMapper.readTree(listAfterUncheck);
        assertThat(tasksAfterUncheck.get(0).get("title").asText()).isEqualTo("Task A");
        assertThat(tasksAfterUncheck.get(0).get("completed").asBoolean()).isFalse();
        assertThat(tasksAfterUncheck.get(1).get("title").asText()).isEqualTo("Task B");
    }

    @Test
    void collaboratorCanEditDeleteAndCompleteTasksOnSharedList() throws Exception {
        String ownerToken = registerAndGetToken("owner-collab@example.com");
        String collaboratorToken = registerAndGetToken("collaborator-collab@example.com");
        shareListWithUser("owner-collab@example.com", "collaborator-collab@example.com");

        String taskResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Owner's task"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        // Collaborator can see the owner's task
        String collaboratorList = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(collaboratorList)).hasSize(1);

        // Collaborator can edit the owner's task
        mockMvc.perform(put("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Edited by collaborator"))))
                .andExpect(status().isOk());

        // Owner sees the collaborator's edit
        String ownerListAfterEdit = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(ownerListAfterEdit).get(0).get("title").asText())
                .isEqualTo("Edited by collaborator");

        // Collaborator can mark the task complete
        mockMvc.perform(patch("/tasks/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("completed", true))))
                .andExpect(status().isOk());

        // Collaborator can delete the task
        mockMvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isNoContent());

        String ownerListAfterDelete = mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(ownerListAfterDelete)).isEmpty();
    }

    @Test
    void nonCollaboratorCannotAccessAnotherUsersTask() throws Exception {
        String ownerToken = registerAndGetToken("owner-private@example.com");
        String strangerToken = registerAndGetToken("stranger-private@example.com");

        String taskResponse = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Private task"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

        mockMvc.perform(put("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Hijacked"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden());
    }
}
