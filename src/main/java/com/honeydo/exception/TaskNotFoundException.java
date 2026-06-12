package com.honeydo.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long taskId) {
        super("No task found with id: " + taskId);
    }
}
