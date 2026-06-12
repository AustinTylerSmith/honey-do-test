package com.honeydo.exception;

public class TaskAccessDeniedException extends RuntimeException {

    public TaskAccessDeniedException(Long taskId) {
        super("You do not have access to task with id: " + taskId);
    }
}
