package com.honeydo.dto;

public class CollaboratorResponse {

    private Long userId;
    private String email;
    private boolean owner;

    public CollaboratorResponse() {
    }

    public CollaboratorResponse(Long userId, String email, boolean owner) {
        this.userId = userId;
        this.email = email;
        this.owner = owner;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }
}
