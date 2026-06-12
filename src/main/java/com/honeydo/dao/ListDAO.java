package com.honeydo.dao;

import com.honeydo.entity.ListEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ListDAO {

    private static final String SELECT_COLUMNS = "id, name, owner_id, created_at";

    private final JdbcTemplate jdbcTemplate;

    public ListDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ListEntity createListForUser(Long userId, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO lists (name, owner_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setLong(2, userId);
            return ps;
        }, keyHolder);

        Long listId = keyHolder.getKey().longValue();
        addUserToList(userId, listId);
        return findById(listId).orElseThrow();
    }

    public Optional<ListEntity> findById(Long listId) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM lists WHERE id = ?",
                ListDAO::mapRow,
                listId
        ).stream().findFirst();
    }

    public List<Long> findListIdsForUser(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT list_id FROM user_list WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    public Optional<Long> findOwnedListId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM lists WHERE owner_id = ?",
                Long.class,
                userId
        ).stream().findFirst();
    }

    public boolean isUserMemberOfList(Long userId, Long listId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_list WHERE user_id = ? AND list_id = ?",
                Integer.class, userId, listId);
        return count != null && count > 0;
    }

    public void addUserToList(Long userId, Long listId) {
        jdbcTemplate.update(
                "INSERT INTO user_list (user_id, list_id) VALUES (?, ?)",
                userId, listId);
    }

    public void removeUserFromList(Long userId, Long listId) {
        jdbcTemplate.update(
                "DELETE FROM user_list WHERE user_id = ? AND list_id = ?",
                userId, listId);
    }

    public List<Long> findCollaboratorUserIds(Long listId) {
        return jdbcTemplate.queryForList(
                "SELECT user_id FROM user_list WHERE list_id = ?",
                Long.class,
                listId
        );
    }

    public boolean isEmailMemberOfList(Long listId, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_list ul " +
                        "JOIN users u ON u.id = ul.user_id " +
                        "WHERE ul.list_id = ? AND u.email = ?",
                Integer.class, listId, email);
        return count != null && count > 0;
    }

    private static ListEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ListEntity(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("owner_id"),
                rs.getString("created_at")
        );
    }
}
