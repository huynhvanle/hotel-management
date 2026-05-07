package com.web.hotel_management.activity.service;

import com.web.hotel_management.activity.entity.ActivityLog;
import com.web.hotel_management.activity.repository.ActivityLogRepository;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public void log(Authentication authentication, String action, String targetType, String targetId, String detail) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) username = "";
        username = username.trim();

        Integer userId = null;
        Integer clientId = null;
        String role = "UNKNOWN";

        try {
            if (!username.isBlank()) {
                User u = userRepository.findByUsername(username).orElse(null);
                if (u != null) {
                    userId = u.getId();
                    if (u.getRole() != null) role = u.getRole().name();
                } else {
                    // Client subject uses phone as principal name
                    Client c = clientRepository.findByPhone(username).orElse(null);
                    if (c != null) {
                        clientId = c.getId();
                        role = "CLIENT";
                    }
                }
            }
        } catch (Exception ignored) {
        }
        activityLogRepository.save(ActivityLog.builder()
                .userId(userId)
                .clientId(clientId)
                .actorRole(role)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .build());
    }
}

