package com.web.hotel_management.user.controller;

import com.web.hotel_management.user.dto.UserDTO;
import com.web.hotel_management.user.dto.UserResponse;
import com.web.hotel_management.user.dto.UserUpdateRequest;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/hotel-management/user")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class UserController {

    private static final String MSG_USER_RETRIEVED = "User retrieved successfully";

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserDTO> userDTOs = users.stream()
                    .map(UserDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(UserResponse.builder()
                    .success(true)
                    .message("Users retrieved successfully")
                    .users(userDTOs)
                    .build());
        } catch (Exception e) {
            log.error("Failed to get all users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UserResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(UserResponse.builder()
                    .success(true)
                    .message(MSG_USER_RETRIEVED)
                    .user(UserDTO.fromEntity(user))
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to get user by id {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(UserResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
            return ResponseEntity.ok(UserResponse.builder()
                    .success(true)
                    .message(MSG_USER_RETRIEVED)
                    .user(UserDTO.fromEntity(user))
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to get user by username {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(UserResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByMail(@PathVariable String email) {
        try {
            User user = userService.findByMail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            return ResponseEntity.ok(UserResponse.builder()
                    .success(true)
                    .message(MSG_USER_RETRIEVED)
                    .user(UserDTO.fromEntity(user))
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to get user by email {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(UserResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            User updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(UserResponse.builder()
                    .success(true)
                    .message("User updated successfully")
                    .user(UserDTO.fromEntity(updatedUser))
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to update user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(UserResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(UserResponse.builder()
                    .success(true)
                    .message("User deleted successfully")
                    .build());
        } catch (RuntimeException e) {
            log.error("Failed to delete user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(UserResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}