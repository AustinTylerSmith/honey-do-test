package com.honeydo.dto;

public class InvitationInfoResponse {

    private Long listId;
    private String listName;
    private String invitedEmail;
    private boolean expired;
    private boolean accepted;

    public InvitationInfoResponse() {
    }

    public InvitationInfoResponse(Long listId, String listName, String invitedEmail, boolean expired, boolean accepted) {
        this.listId = listId;
        this.listName = listName;
        this.invitedEmail = invitedEmail;
        this.expired = expired;
        this.accepted = accepted;
    }

    public Long getListId() {
        return listId;
    }

    public void setListId(Long listId) {
        this.listId = listId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
