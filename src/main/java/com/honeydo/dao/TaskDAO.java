package com.honeydo.dao;

import com.honeydo.entity.TaskEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TaskDAO {

    private static final String SELECT_COLUMNS =
            "id, user_id, title, completed, created_at, due_date, estimated_minutes";

    private final JdbcTemplate jdbcTemplate;

    public TaskDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskEntity> findAllByListIds(List<Long> listIds) {
        if (listIds.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = listIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        return jdbcTemplate.query(
                "SELECT DISTINCT t.id, t.user_id, t.title, t.completed, t.created_at, t.due_date, t.estimated_minutes " +
                        "FROM tasks t " +
                        "JOIN task_list tl ON tl.task_id = t.id " +
                        "WHERE tl.list_id IN (" + placeholders + ") " +
                        "ORDER BY t.completed ASC, t.due_date ASC",
                TaskDAO::mapRow,
                listIds.toArray()
        );
    }

    public Optional<TaskEntity> findById(Long taskId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM tasks WHERE id = ?",
                TaskDAO::mapRow,
                taskId
        ).stream().findFirst();
    }

    public Optional<Long> findListIdForTask(Long taskId) {
        return jdbcTemplate.queryForList(
                "SELECT list_id FROM task_list WHERE task_id = ?",
                Long.class,
                taskId
        ).stream().findFirst();
    }

    public TaskEntity insert(Long userId, Long listId, String title, String dueDate, Integer estimatedMinutes) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO tasks (user_id, title, completed, due_date, estimated_minutes) VALUES (?, ?, 0, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, title);
            if (dueDate != null) {
                ps.setString(3, dueDate);
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }
            if (estimatedMinutes != null) {
                ps.setInt(4, estimatedMinutes);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);

        Long taskId = keyHolder.getKey().longValue();
        jdbcTemplate.update("INSERT INTO task_list (task_id, list_id) VALUES (?, ?)", taskId, listId);
        return findById(taskId).orElseThrow();
    }

    public void update(Long taskId, String title, String dueDate, Integer estimatedMinutes) {
        jdbcTemplate.update(
                "UPDATE tasks SET title = ?, due_date = ?, estimated_minutes = ? WHERE id = ?",
                title, dueDate, estimatedMinutes, taskId
        );
    }

    public void setCompleted(Long taskId, boolean completed) {
        jdbcTemplate.update(
                "UPDATE tasks SET completed = ? WHERE id = ?",
                completed, taskId
        );
    }

    public void delete(Long taskId) {
        jdbcTemplate.update("DELETE FROM task_list WHERE task_id = ?", taskId);
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", taskId);
    }

    private static TaskEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        int estimatedMinutes = rs.getInt("estimated_minutes");
        return new TaskEntity(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                rs.getBoolean("completed"),
                rs.getString("created_at"),
                rs.getString("due_date"),
                rs.wasNull() ? null : estimatedMinutes
        );
    }
}
