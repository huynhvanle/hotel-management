package com.web.hotel_management.user.service;

import com.web.hotel_management.user.dto.UserUpdateRequest;
import com.web.hotel_management.user.dto.SelfUpdateRequest;
import com.web.hotel_management.user.dto.UserCreateRequest;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByMail(String mail) {
        return userRepository.findByMail(mail);
    }

    public User getUserById(@NonNull Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByMail(String mail) {
        return userRepository.existsByMail(mail);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User createUser(UserCreateRequest request) {
        String username = request.getUsername().trim();
        String mail = request.getMail().trim();
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByMail(mail)) {
            throw new RuntimeException("Email is already registered");
        }

        String role = request.getPosition() != null ? request.getPosition().trim().toUpperCase() : "USER";
        role = "ADMIN".equals(role) ? "ADMIN" : "USER";

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .position(role)
                .mail(mail)
                .description(request.getDescription())
                .build();
        return userRepository.save(user);
    }

    public User updateUser(Integer id, UserUpdateRequest request) {
        User user = getUserById(id);
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition().trim().toUpperCase());
        }
        if (request.getMail() != null) {
            user.setMail(request.getMail());
        }
        if (request.getDescription() != null) {
            user.setDescription(request.getDescription());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        log.info("User updated with id: {}", id);
        return userRepository.save(user);
    }

    public User updateSelf(String username, SelfUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getMail() != null) {
            user.setMail(request.getMail());
        }
        if (request.getDescription() != null) {
            user.setDescription(request.getDescription());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        log.info("Self user updated: {}", username);
        return userRepository.save(user);
    }

    public void deleteUser(@NonNull Integer id) {
        User target = getUserById(id);
        String role = target.getPosition() != null ? target.getPosition().trim().toUpperCase() : "USER";
        role = "ADMIN".equals(role) ? "ADMIN" : "USER";
        if ("ADMIN".equals(role)) {
            throw new RuntimeException("Cannot delete ADMIN user");
        }
        userRepository.deleteById(id);
        log.info("User deleted with id: {}", id);
    }
}
