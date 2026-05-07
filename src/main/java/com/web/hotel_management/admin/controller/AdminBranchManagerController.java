package com.web.hotel_management.admin.controller;

import com.web.hotel_management.admin.dto.AdminManagerActionResponse;
import com.web.hotel_management.admin.dto.AdminManagerItemResponse;
import com.web.hotel_management.admin.dto.AdminManagerUpsertRequest;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import com.web.hotel_management.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/branch-managers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBranchManagerController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    private static AdminManagerItemResponse toItem(User u) {
        return AdminManagerItemResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .branchId(u.getBranch() != null ? u.getBranch().getId() : null)
                .branchName(u.getBranch() != null ? u.getBranch().getName() : null)
                .build();
    }

    @GetMapping
    public List<AdminManagerItemResponse> list(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "fullName", required = false) String fullName,
            @RequestParam(name = "username", required = false) String username,
            Authentication authentication
    ) {
        String qPhone = phone == null || phone.trim().isEmpty() ? null : phone.trim();
        String qName = fullName == null || fullName.trim().isEmpty() ? null : fullName.trim();
        String qUser = username == null || username.trim().isEmpty() ? null : username.trim();
        log.info("[ADMIN] search branch managers: id={}, phone={}, fullName={}, username={}", id, qPhone, qName, qUser);
        activityLogService.log(authentication, "ADMIN_MANAGER_SEARCH", "MANAGER", null,
                "id=" + id + ",phone=" + qPhone + ",fullName=" + qName + ",username=" + qUser);

        return userRepository.findByRole(UserRole.BRANCH_MANAGER).stream()
                .filter(u -> id == null || u.getId().equals(id))
                .filter(u -> qPhone == null || (u.getPhone() != null && u.getPhone().contains(qPhone)))
                .filter(u -> qName == null || (u.getFullName() != null && u.getFullName().toLowerCase().contains(qName.toLowerCase())))
                .filter(u -> qUser == null || (u.getUsername() != null && u.getUsername().toLowerCase().contains(qUser.toLowerCase())))
                .map(AdminBranchManagerController::toItem)
                .toList();
    }

    @PostMapping
    public AdminManagerItemResponse create(@Valid @RequestBody AdminManagerUpsertRequest req, Authentication authentication) {
        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        if (userRepository.existsByUsername(username)) throw new RuntimeException("Tên đăng nhập đã được sử dụng.");

        String phone = req.getPhone() != null ? req.getPhone().trim() : "";
        if (userRepository.existsByPhone(phone)) throw new RuntimeException("Số điện thoại đã tồn tại.");

        String rawPass = req.getPassword() != null ? req.getPassword().trim() : "";
        if (rawPass.length() < 8) throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự.");

        Integer branchId = req.getBranchId();
        Hotel branch = hotelRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));
        if (userRepository.existsByBranch_IdAndRole(branchId, UserRole.BRANCH_MANAGER)) {
            throw new RuntimeException("Chi nhánh này đã có đủ nhân sự quản lí theo định biên.");
        }

        User created = userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPass))
                .fullName(req.getFullName().trim())
                .phone(phone)
                .role(UserRole.BRANCH_MANAGER)
                .branch(branch)
                .build());

        log.info("[ADMIN] create branch manager: id={}, username={}, branchId={}", created.getId(), created.getUsername(), branchId);
        activityLogService.log(authentication, "ADMIN_MANAGER_CREATE", "MANAGER", String.valueOf(created.getId()),
                "username=" + created.getUsername() + ",branchId=" + branchId);
        return toItem(created);
    }

    @PutMapping("/{id}")
    public AdminManagerItemResponse update(@PathVariable Integer id, @Valid @RequestBody AdminManagerUpsertRequest req, Authentication authentication) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        if (target.getRole() != UserRole.BRANCH_MANAGER) throw new RuntimeException("Chỉ cho phép sửa tài khoản Manager.");

        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        if (!username.equals(target.getUsername())) {
            if (userRepository.existsByUsername(username)) throw new RuntimeException("Tên đăng nhập đã được sử dụng.");
            target.setUsername(username);
        }

        String phone = req.getPhone() != null ? req.getPhone().trim() : "";
        if (!phone.equals(target.getPhone()) && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Số điện thoại đã tồn tại.");
        }
        target.setFullName(req.getFullName().trim());
        target.setPhone(phone);

        String rawPass = req.getPassword() != null ? req.getPassword().trim() : "";
        if (!rawPass.isBlank()) {
            if (rawPass.length() < 8) throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự.");
            target.setPassword(passwordEncoder.encode(rawPass));
        }

        Integer branchId = req.getBranchId();
        Hotel branch = hotelRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));
        // One manager per branch (except the same manager already assigned to it).
        boolean occupiedByOther = userRepository.findByRole(UserRole.BRANCH_MANAGER).stream()
                .anyMatch(u -> !u.getId().equals(target.getId())
                        && u.getBranch() != null
                        && u.getBranch().getId() != null
                        && u.getBranch().getId().equals(branchId));
        if (occupiedByOther) throw new RuntimeException("Chi nhánh này đã có đủ nhân sự quản lí theo định biên.");
        target.setBranch(branch);
        target.setRole(UserRole.BRANCH_MANAGER);

        User saved = userRepository.save(target);
        log.info("[ADMIN] update branch manager: id={}, branchId={}", id, branchId);
        activityLogService.log(authentication, "ADMIN_MANAGER_UPDATE", "MANAGER", String.valueOf(id),
                "username=" + target.getUsername() + ",branchId=" + branchId);
        return toItem(saved);
    }

    @DeleteMapping("/{id}")
    public AdminManagerActionResponse delete(@PathVariable Integer id, Authentication authentication) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        if (target.getRole() != UserRole.BRANCH_MANAGER) throw new RuntimeException("Chỉ cho phép xoá tài khoản Manager.");

        // Revoke permanently: keep username occupied, invalidate password and remove branch/role.
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        String randomSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        target.setPassword(passwordEncoder.encode(randomSecret));
        target.setRole(UserRole.SYSTEM);
        target.setBranch(null);
        userRepository.save(target);

        log.info("[ADMIN] revoke branch manager: id={}, username={}", id, target.getUsername());
        activityLogService.log(authentication, "ADMIN_MANAGER_DELETE", "MANAGER", String.valueOf(id),
                "username=" + target.getUsername());
        return AdminManagerActionResponse.builder()
                .id(id)
                .message("Đã thu hồi quyền quản trị vĩnh viễn.")
                .build();
    }
}

