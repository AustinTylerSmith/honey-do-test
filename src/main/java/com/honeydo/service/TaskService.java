package com.honeydo.service;

import com.honeydo.dao.ListDAO;
import com.honeydo.dao.TaskDAO;
import com.honeydo.dao.UserDAO;
import com.honeydo.dto.TaskRequest;
import com.honeydo.dto.TaskResponse;
import com.honeydo.entity.TaskEntity;
import com.honeydo.entity.UserEntity;
import com.honeydo.exception.TaskAccessDeniedException;
import com.honeydo.exception.TaskNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskDAO taskDAO;
    private final UserDAO userDAO;
    private final ListDAO listDAO;

    public TaskService(TaskDAO taskDAO, UserDAO userDAO, ListDAO listDAO) {
        this.taskDAO = taskDAO;
        this.userDAO = userDAO;
        this.listDAO = listDAO;
    }

    public List<TaskResponse> getTasksForUser(String email) {
        UserEntity user = findUserByEmail(email);

        List<Long> listIds;
        try {
            listIds = listDAO.findListIdsForUser(user.getId());
        } catch (DataAccessException e) {
            log.error("Failed to find list memberships for user {}", user.getId(), e);
            throw e;
        }

        List<TaskEntity> tasks;
        try {
            tasks = taskDAO.findAllByListIds(listIds);
        } catch (DataAccessException e) {
            log.error("Failed to find tasks for lists {}", listIds, e);
            throw e;
        }

        return tasks.stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    public TaskResponse createTask(String email, TaskRequest request) {
        UserEntity user = findUserByEmail(email);

        Long listId;
        try {
            listId = listDAO.findOwnedListId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("User " + user.getId() + " has no owned list"));
        } catch (DataAccessException e) {
            log.error("Failed to find owned list for user {}", user.getId(), e);
            throw e;
        }

        TaskEntity created;
        try {
            created = taskDAO.insert(user.getId(), listId, request.getTitle(), request.getDueDate(), request.getEstimatedMinutes());
        } catch (DataAccessException e) {
            log.error("Failed to create task for user {}", user.getId(), e);
            throw e;
        }

        return TaskResponse.fromEntity(created);
    }

    public TaskResponse updateTask(String email, Long taskId, TaskRequest request) {
        UserEntity user = findUserByEmail(email);
        ensureUserCanAccessTask(user, taskId);

        try {
            taskDAO.update(taskId, request.getTitle(), request.getDueDate(), request.getEstimatedMinutes());
        } catch (DataAccessException e) {
            log.error("Failed to update task {}", taskId, e);
            throw e;
        }

        return TaskResponse.fromEntity(taskDAO.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId)));
    }

    public TaskResponse setTaskCompleted(String email, Long taskId, boolean completed) {
        UserEntity user = findUserByEmail(email);
        ensureUserCanAccessTask(user, taskId);

        try {
            taskDAO.setCompleted(taskId, completed);
        } catch (DataAccessException e) {
            log.error("Failed to update completion status for task {}", taskId, e);
            throw e;
        }

        return TaskResponse.fromEntity(taskDAO.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId)));
    }

    public void deleteTask(String email, Long taskId) {
        UserEntity user = findUserByEmail(email);
        ensureUserCanAccessTask(user, taskId);

        try {
            taskDAO.delete(taskId);
        } catch (DataAccessException e) {
            log.error("Failed to delete task {}", taskId, e);
            throw e;
        }
    }

    private UserEntity findUserByEmail(String email) {
        try {
            return userDAO.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
        } catch (DataAccessException e) {
            log.error("Failed to find user with email {}", email, e);
            throw e;
        }
    }

    private void ensureUserCanAccessTask(UserEntity user, Long taskId) {
        Long listId;
        try {
            listId = taskDAO.findListIdForTask(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
        } catch (DataAccessException e) {
            log.error("Failed to find list for task {}", taskId, e);
            throw e;
        }

        boolean isMember;
        try {
            isMember = listDAO.isUserMemberOfList(user.getId(), listId);
        } catch (DataAccessException e) {
            log.error("Failed to check list membership for user {} and list {}", user.getId(), listId, e);
            throw e;
        }

        if (!isMember) {
            throw new TaskAccessDeniedException(taskId);
        }
    }
}
