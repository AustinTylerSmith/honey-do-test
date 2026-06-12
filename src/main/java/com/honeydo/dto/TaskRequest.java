package com.honeydo.dto;

import jakarta.validation.constraints.NotBlank;

public class TaskRequest {

    @NotBlank
    private String title;

    private String dueDate;

    private Integer estimatedMinutes;

    public TaskRequest() {
    }

    public TaskRequest(String title, String dueDate, Integer estimatedMinutes) {
        this.title = title;
        this.dueDate = dueDate;
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
