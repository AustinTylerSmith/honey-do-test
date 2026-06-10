package com.honeydo.service;

import com.honeydo.dao.TaskDAO;
import com.honeydo.dao.UserDAO;
import com.honeydo.dto.TaskResponse;
import com.honeydo.entity.TaskEntity;
import com.honeydo.entity.UserEntity;
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

    public TaskService(TaskDAO taskDAO, UserDAO userDAO) {
        this.taskDAO = taskDAO;
        this.userDAO = userDAO;
    }

    public List<TaskResponse> getTasksForUser(String email) {
        UserEntity user;
        try {
            user = userDAO.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
        } catch (DataAccessException e) {
            log.error("Failed to find user with email {}", email, e);
            throw e;
        }

        List<TaskEntity> tasks;
        try {
            tasks = taskDAO.findAllByUserId(user.getId());
        } catch (DataAccessException e) {
            log.error("Failed to find tasks for user {}", user.getId(), e);
            throw e;
        }

        return tasks.stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }
}
