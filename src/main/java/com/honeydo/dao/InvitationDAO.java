package com.honeydo.dao;

import com.honeydo.entity.InvitationEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class InvitationDAO {

    private static final String SELECT_COLUMNS =
            "id, list_id, invited_by_user_id, email, token, created_at, expires_at, accepted_at";

    private final JdbcTemplate jdbcTemplate;

    public InvitationDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InvitationEntity create(Long listId, Long invitedByUserId, String email, String token) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO invitations (list_id, invited_by_user_id, email, token, expires_at) " +
                            "VALUES (?, ?, ?, ?, datetime('now', '+7 days'))",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, listId);
            ps.setLong(2, invitedByUserId);
            ps.setString(3, email);
            ps.setString(4, token);
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        return findById(id).orElseThrow();
    }

    public Optional<InvitationEntity> findById(Long id) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM invitations WHERE id = ?",
                InvitationDAO::mapRow,
                id
        ).stream().findFirst();
    }

    public Optional<InvitationEntity> findByToken(String token) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM invitations WHERE token = ?",
                InvitationDAO::mapRow,
                token
        ).stream().findFirst();
    }

    public void markAccepted(Long id) {
        jdbcTemplate.update(
                "UPDATE invitations SET accepted_at = datetime('now') WHERE id = ?",
                id);
    }

    public boolean existsPendingForListAndEmail(Long listId, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM invitations WHERE list_id = ? AND email = ? " +
                        "AND accepted_at IS NULL AND expires_at > datetime('now')",
                Integer.class, listId, email);
        return count != null && count > 0;
    }

    private static InvitationEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new InvitationEntity(
                rs.getLong("id"),
                rs.getLong("list_id"),
                rs.getLong("invited_by_user_id"),
                rs.getString("email"),
                rs.getString("token"),
                rs.getString("created_at"),
                rs.getString("expires_at"),
                rs.getString("accepted_at")
        );
    }
}
