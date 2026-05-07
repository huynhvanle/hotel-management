package com.web.hotel_management.admin.controller;

import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import com.web.hotel_management.user.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/managers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagerSearchController {

    private final UserRepository userRepository;

    @GetMapping
    public List<ManagerItem> search(@RequestParam(name = "q", required = false) String q) {
        String qq = q == null ? "" : q.trim();
        log.info("[ADMIN] search managers: q={}", qq);
        // keep it simple: filter in memory since dataset is small; can optimize later.
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.BRANCH_MANAGER)
                .filter(u -> qq.isBlank()
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(qq.toLowerCase()))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(qq.toLowerCase()))
                        || (u.getPhone() != null && u.getPhone().contains(qq))
                )
                .map(ManagerItem::from)
                .toList();
    }

    @Data
    @Builder
    public static class ManagerItem {
        private Integer id;
        private String username;
        private String fullName;
        private String phone;
        private Integer branchId;
        private String branchName;

        public static ManagerItem from(User u) {
            return ManagerItem.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .fullName(u.getFullName())
                    .phone(u.getPhone())
                    .branchId(u.getBranch() != null ? u.getBranch().getId() : null)
                    .branchName(u.getBranch() != null ? u.getBranch().getName() : null)
                    .build();
        }
    }
}

