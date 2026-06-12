package com.honeydo.controller;

import com.honeydo.dto.AcceptInvitationResponse;
import com.honeydo.dto.CollaboratorResponse;
import com.honeydo.dto.InvitationInfoResponse;
import com.honeydo.dto.InviteRequest;
import com.honeydo.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/lists/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public void inviteToList(Authentication authentication, @Valid @RequestBody InviteRequest request) {
        invitationService.inviteToList(authentication.getName(), request.getEmail());
    }

    @GetMapping("/lists/collaborators")
    public List<CollaboratorResponse> listCollaborators(Authentication authentication) {
        return invitationService.listCollaborators(authentication.getName());
    }

    @DeleteMapping("/lists/collaborators/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAccess(Authentication authentication, @PathVariable Long userId) {
        invitationService.revokeAccess(authentication.getName(), userId);
    }

    @GetMapping("/invitations/{token}")
    public InvitationInfoResponse getInvitationInfo(@PathVariable String token) {
        return invitationService.getInvitationInfo(token);
    }

    @PostMapping("/invitations/{token}/accept")
    public AcceptInvitationResponse acceptInvitation(Authentication authentication, @PathVariable String token) {
        return invitationService.acceptInvitation(token, authentication.getName());
    }
}
