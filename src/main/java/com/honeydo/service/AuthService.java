package com.honeydo.service;

import com.honeydo.dao.ListDAO;
import com.honeydo.dao.UserDAO;
import com.honeydo.dto.AuthResponse;
import com.honeydo.dto.LoginRequest;
import com.honeydo.dto.RegisterRequest;
import com.honeydo.entity.UserEntity;
import com.honeydo.exception.EmailAlreadyExistsException;
import com.honeydo.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDAO userDAO;
    private final ListDAO listDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserDAO userDAO, ListDAO listDAO, PasswordEncoder passwordEncoder,
                        JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userDAO = userDAO;
        this.listDAO = listDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        boolean emailExists;
        try {
            emailExists = userDAO.existsByEmail(request.getEmail());
        } catch (DataAccessException e) {
            log.error("Failed to check if email {} is already registered", request.getEmail(), e);
            throw e;
        }

        if (emailExists) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        UserEntity user;
        try {
            user = userDAO.save(request.getEmail(), passwordHash);
        } catch (DataAccessException e) {
            log.error("Failed to save new user with email {}", request.getEmail(), e);
            throw e;
        }

        try {
            listDAO.createListForUser(user.getId(), "My Tasks");
        } catch (DataAccessException e) {
            log.error("Failed to create default list for user {}", user.getId(), e);
            throw e;
        }

        return new AuthResponse(jwtService.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        return new AuthResponse(jwtService.generateToken(request.getEmail()));
    }
}
