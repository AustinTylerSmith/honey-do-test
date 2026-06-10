package com.honeydo.controller;

import com.honeydo.dto.TaskResponse;
import com.honeydo.service.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getTasks(Authentication authentication) {
        return taskService.getTasksForUser(authentication.getName());
    }
}
