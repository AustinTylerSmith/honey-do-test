package com.honeydo.security;

import com.honeydo.dao.UserDAO;
import com.honeydo.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserDAO userDAO;

    public CustomUserDetailsService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user;
        try {
            user = userDAO.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
        } catch (DataAccessException e) {
            log.error("Failed to find user with email {}", email, e);
            throw e;
        }

        return new User(user.getEmail(), user.getPasswordHash(), Collections.emptyList());
    }
}
