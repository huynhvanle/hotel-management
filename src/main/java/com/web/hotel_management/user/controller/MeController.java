package com.web.hotel_management.user.controller;

import com.web.hotel_management.user.dto.SelfUpdateRequest;
import com.web.hotel_management.user.dto.UserDTO;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeController {
    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserDTO me(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return UserDTO.fromEntity(user);
    }

    @PutMapping
    public UserDTO updateMe(@Valid @RequestBody SelfUpdateRequest request, Authentication authentication) {
        String username = authentication.getName();
        User updated = userService.updateSelf(username, request);
        return UserDTO.fromEntity(updated);
    }
}

