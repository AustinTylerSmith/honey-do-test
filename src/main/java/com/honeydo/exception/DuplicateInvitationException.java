package com.honeydo.exception;

public class DuplicateInvitationException extends RuntimeException {

    public DuplicateInvitationException() {
        super("This user already has access to this list");
    }
}
