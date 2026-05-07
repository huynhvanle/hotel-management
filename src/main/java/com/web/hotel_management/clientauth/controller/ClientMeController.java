package com.web.hotel_management.clientauth.controller;

import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.clientauth.dto.ClientProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientMeController {

    private final ClientRepository clientRepository;
    private final ActivityLogService activityLogService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientProfileResponse me(Authentication authentication) {
        String phone = authentication != null ? authentication.getName() : null;
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Invalid client identity.");
        }
        activityLogService.log(authentication, "CLIENT_PROFILE_VIEW", "CLIENT", null, null);
        Client c = clientRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new RuntimeException("Client not found."));
        return ClientProfileResponse.fromEntity(c);
    }
}

