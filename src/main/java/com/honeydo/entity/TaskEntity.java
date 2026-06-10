package com.honeydo.entity;

public class TaskEntity {

    private Long id;
    private Long userId;
    private String title;
    private boolean completed;
    private String createdAt;

    public TaskEntity() {
    }

    public TaskEntity(Long id, Long userId, String title, boolean completed, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
