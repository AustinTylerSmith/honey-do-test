package com.honeydo.dao;

import com.honeydo.entity.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UserDAO {

    private static final String SELECT_COLUMNS = "id, email, password_hash, created_at";

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserEntity> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE email = ?",
                UserDAO::mapRow,
                email
        ).stream().findFirst();
    }

    public Optional<UserEntity> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE id = ?",
                UserDAO::mapRow,
                id
        ).stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public UserEntity save(String email, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, password_hash) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return findById(id).orElseThrow();
    }

    private static UserEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new UserEntity(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("created_at")
        );
    }
}
