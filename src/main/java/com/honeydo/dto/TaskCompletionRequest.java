package com.honeydo.dto;

public class TaskCompletionRequest {

    private boolean completed;

    public TaskCompletionRequest() {
    }

    public TaskCompletionRequest(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
