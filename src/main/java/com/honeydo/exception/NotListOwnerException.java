package com.honeydo.exception;

public class NotListOwnerException extends RuntimeException {

    public NotListOwnerException() {
        super("Only the list owner can perform this action");
    }
}
