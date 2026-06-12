package com.honeydo.service;

import com.honeydo.dao.InvitationDAO;
import com.honeydo.dao.ListDAO;
import com.honeydo.dao.UserDAO;
import com.honeydo.dto.AcceptInvitationResponse;
import com.honeydo.dto.CollaboratorResponse;
import com.honeydo.dto.InvitationInfoResponse;
import com.honeydo.entity.InvitationEntity;
import com.honeydo.entity.ListEntity;
import com.honeydo.entity.UserEntity;
import com.honeydo.exception.DuplicateInvitationException;
import com.honeydo.exception.InvitationExpiredException;
import com.honeydo.exception.InvitationNotFoundException;
import com.honeydo.exception.MailConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);
    private static final DateTimeFormatter SQLITE_DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneOffset.UTC);

    private final InvitationDAO invitationDAO;
    private final ListDAO listDAO;
    private final UserDAO userDAO;
    private final EmailService emailService;
    private final String frontendUrl;

    public InvitationService(InvitationDAO invitationDAO, ListDAO listDAO, UserDAO userDAO,
                              EmailService emailService,
                              @Value("${app.frontend-url}") String frontendUrl) {
        this.invitationDAO = invitationDAO;
        this.listDAO = listDAO;
        this.userDAO = userDAO;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public void inviteToList(String ownerEmail, String recipientEmail) {
        UserEntity owner = findUserByEmail(ownerEmail);
        ListEntity list = findOwnedList(owner.getId());

        boolean alreadyMember;
        try {
            alreadyMember = listDAO.isEmailMemberOfList(list.getId(), recipientEmail);
        } catch (DataAccessException e) {
            log.error("Failed to check membership for email {} on list {}", recipientEmail, list.getId(), e);
            throw e;
        }

        if (alreadyMember) {
            throw new DuplicateInvitationException();
        }

        String token = UUID.randomUUID().toString();
        InvitationEntity invitation;
        try {
            invitation = invitationDAO.create(list.getId(), owner.getId(), recipientEmail, token);
        } catch (DataAccessException e) {
            log.error("Failed to create invitation for list {} and email {}", list.getId(), recipientEmail, e);
            throw e;
        }

        String link = frontendUrl + "/invite/" + invitation.getToken();
        try {
            emailService.sendEmail(
                    recipientEmail,
                    "You've been invited to join \"" + list.getName() + "\" on Honey Do",
                    "You've been invited to collaborate on the list \"" + list.getName() + "\".\n\n" +
                            "Click the link below to join:\n" + link + "\n\n" +
                            "This invitation expires in 7 days."
            );
        } catch (MailConfigurationException e) {
            log.error("Invitation {} created for {} but email could not be sent because SMTP is not configured",
                    invitation.getId(), recipientEmail, e);
        }
    }

    public List<CollaboratorResponse> listCollaborators(String ownerEmail) {
        UserEntity owner = findUserByEmail(ownerEmail);
        ListEntity list = findOwnedList(owner.getId());

        List<Long> userIds;
        try {
            userIds = listDAO.findCollaboratorUserIds(list.getId());
        } catch (DataAccessException e) {
            log.error("Failed to find collaborators for list {}", list.getId(), e);
            throw e;
        }

        return userIds.stream()
                .map(userId -> {
                    UserEntity user = userDAO.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
                    return new CollaboratorResponse(user.getId(), user.getEmail(), user.getId().equals(list.getOwnerId()));
                })
                .toList();
    }

    public void revokeAccess(String ownerEmail, Long collaboratorUserId) {
        UserEntity owner = findUserByEmail(ownerEmail);
        ListEntity list = findOwnedList(owner.getId());

        try {
            listDAO.removeUserFromList(collaboratorUserId, list.getId());
        } catch (DataAccessException e) {
            log.error("Failed to revoke access for user {} on list {}", collaboratorUserId, list.getId(), e);
            throw e;
        }
    }

    public InvitationInfoResponse getInvitationInfo(String token) {
        InvitationEntity invitation = findInvitationByToken(token);
        ListEntity list = findListById(invitation.getListId());

        boolean expired = isExpired(invitation);
        boolean accepted = invitation.getAcceptedAt() != null;

        return new InvitationInfoResponse(list.getId(), list.getName(), invitation.getEmail(), expired, accepted);
    }

    public AcceptInvitationResponse acceptInvitation(String token, String authenticatedUserEmail) {
        InvitationEntity invitation = findInvitationByToken(token);

        if (isExpired(invitation)) {
            throw new InvitationExpiredException();
        }

        UserEntity user = findUserByEmail(authenticatedUserEmail);
        ListEntity list = findListById(invitation.getListId());

        boolean alreadyMember;
        try {
            alreadyMember = listDAO.isUserMemberOfList(user.getId(), list.getId());
        } catch (DataAccessException e) {
            log.error("Failed to check membership for user {} on list {}", user.getId(), list.getId(), e);
            throw e;
        }

        if (!alreadyMember) {
            try {
                listDAO.addUserToList(user.getId(), list.getId());
            } catch (DataAccessException e) {
                log.error("Failed to add user {} to list {}", user.getId(), list.getId(), e);
                throw e;
            }
        }

        if (invitation.getAcceptedAt() == null) {
            try {
                invitationDAO.markAccepted(invitation.getId());
            } catch (DataAccessException e) {
                log.error("Failed to mark invitation {} as accepted", invitation.getId(), e);
                throw e;
            }
        }

        return new AcceptInvitationResponse(list.getId(), list.getName());
    }

    private boolean isExpired(InvitationEntity invitation) {
        Instant expiresAt = parseSqliteDatetime(invitation.getExpiresAt());
        return Instant.now().isAfter(expiresAt);
    }

    private Instant parseSqliteDatetime(String value) {
        return java.time.LocalDateTime.parse(value, SQLITE_DATETIME_FORMAT).toInstant(java.time.ZoneOffset.UTC);
    }

    private InvitationEntity findInvitationByToken(String token) {
        try {
            return invitationDAO.findByToken(token).orElseThrow(InvitationNotFoundException::new);
        } catch (DataAccessException e) {
            log.error("Failed to find invitation by token", e);
            throw e;
        }
    }

    private ListEntity findListById(Long listId) {
        try {
            return listDAO.findById(listId).orElseThrow(() -> new IllegalStateException("List " + listId + " not found"));
        } catch (DataAccessException e) {
            log.error("Failed to find list {}", listId, e);
            throw e;
        }
    }

    private ListEntity findOwnedList(Long userId) {
        Long listId;
        try {
            listId = listDAO.findOwnedListId(userId)
                    .orElseThrow(() -> new IllegalStateException("User " + userId + " has no owned list"));
        } catch (DataAccessException e) {
            log.error("Failed to find owned list for user {}", userId, e);
            throw e;
        }
        return findListById(listId);
    }

    private UserEntity findUserByEmail(String email) {
        try {
            return userDAO.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        } catch (DataAccessException e) {
            log.error("Failed to find user by email {}", email, e);
            throw e;
        }
    }
}
