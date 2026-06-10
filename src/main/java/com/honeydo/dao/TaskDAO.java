package com.honeydo.dao;

import com.honeydo.entity.TaskEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskDAO {

    private final JdbcTemplate jdbcTemplate;

    public TaskDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskEntity> findAllByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, title, completed, created_at FROM tasks WHERE user_id = ? ORDER BY id",
                (rs, rowNum) -> new TaskEntity(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("title"),
                        rs.getBoolean("completed"),
                        rs.getString("created_at")
                ),
                userId
        );
    }
}
