package com.honeydo.dto;

import com.honeydo.entity.TaskEntity;

public class TaskResponse {

    private Long id;
    private String title;
    private boolean completed;
    private String dueDate;
    private Integer estimatedMinutes;

    public TaskResponse() {
    }

    public TaskResponse(Long id, String title, boolean completed, String dueDate, Integer estimatedMinutes) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.dueDate = dueDate;
        this.estimatedMinutes = estimatedMinutes;
    }

    public static TaskResponse fromEntity(TaskEntity entity) {
        return new TaskResponse(entity.getId(), entity.getTitle(), entity.isCompleted(),
                entity.getDueDate(), entity.getEstimatedMinutes());
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

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
}
