package com.honeydo.dto;

public class AcceptInvitationResponse {

    private Long listId;
    private String listName;

    public AcceptInvitationResponse() {
    }

    public AcceptInvitationResponse(Long listId, String listName) {
        this.listId = listId;
        this.listName = listName;
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
}
