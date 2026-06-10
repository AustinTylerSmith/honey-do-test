package com.honeydo.dto;

import com.honeydo.entity.TaskEntity;

public class TaskResponse {

    private Long id;
    private String title;
    private boolean completed;

    public TaskResponse() {
    }

    public TaskResponse(Long id, String title, boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    public static TaskResponse fromEntity(TaskEntity entity) {
        return new TaskResponse(entity.getId(), entity.getTitle(), entity.isCompleted());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
