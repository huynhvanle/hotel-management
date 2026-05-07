package com.web.hotel_management.branch.controller;

import com.web.hotel_management.branch.dto.BranchReceptionistActionResponse;
import com.web.hotel_management.branch.dto.BranchReceptionistItemResponse;
import com.web.hotel_management.branch.dto.BranchReceptionistUpsertRequest;
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
@RequestMapping("/api/branch/receptionists")
@RequiredArgsConstructor
public class BranchReceptionistController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    private Integer resolveBranchId(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) throw new RuntimeException("Thiếu thông tin đăng nhập.");
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));

        if (me.getRole() == UserRole.ADMIN) {
            Integer bid = me.getBranch() != null ? me.getBranch().getId() : null;
            if (bid == null) throw new RuntimeException("Admin chưa được gán chi nhánh (tạm thời).");
            return bid;
        }

        Integer bid = me.getBranch() != null ? me.getBranch().getId() : null;
        if (bid == null) throw new RuntimeException("Tài khoản chưa được gán chi nhánh.");
        return bid;
    }

    private static BranchReceptionistItemResponse toItem(User u) {
        return BranchReceptionistItemResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .branchId(u.getBranch() != null ? u.getBranch().getId() : null)
                .branchName(u.getBranch() != null ? u.getBranch().getName() : null)
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public List<BranchReceptionistItemResponse> list(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "fullName", required = false) String fullName,
            Authentication authentication
    ) {
        Integer branchId = resolveBranchId(authentication);
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[BRANCH] search receptionists: user={}, branchId={}, id={}, phone={}, fullName={}",
                u, branchId, id, phone, fullName);
        activityLogService.log(authentication, "RECEPTIONIST_SEARCH", "BRANCH", String.valueOf(branchId),
                "id=" + id + ",phone=" + phone + ",fullName=" + fullName);

        String qPhone = phone == null || phone.trim().isEmpty() ? null : phone.trim();
        String qName = fullName == null || fullName.trim().isEmpty() ? null : fullName.trim();
        return userRepository.searchByBranchAndRole(branchId, UserRole.RECEPTIONIST, id, qPhone, qName).stream()
                .map(BranchReceptionistController::toItem)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public BranchReceptionistItemResponse create(@Valid @RequestBody BranchReceptionistUpsertRequest req, Authentication authentication) {
        Integer branchId = resolveBranchId(authentication);
        String u = authentication != null ? authentication.getName() : "unknown";

        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        if (username.isBlank()) throw new RuntimeException("Vui lòng nhập tên đăng nhập.");
        if (userRepository.existsByUsername(username)) throw new RuntimeException("Tên đăng nhập đã tồn tại.");

        String phone = req.getPhone() != null ? req.getPhone().trim() : "";
        if (userRepository.existsByPhone(phone)) throw new RuntimeException("Số điện thoại đã tồn tại.");

        Hotel branch = hotelRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));

        String rawPass = req.getPassword() != null ? req.getPassword().trim() : "";
        if (rawPass.length() < 8) throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự.");

        User created = userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPass))
                .fullName(req.getFullName().trim())
                .phone(phone)
                .role(UserRole.RECEPTIONIST)
                .branch(branch)
                .build());

        log.info("[BRANCH] create receptionist: user={}, branchId={}, receptionistId={}, username={}", u, branchId, created.getId(), created.getUsername());
        activityLogService.log(authentication, "RECEPTIONIST_CREATE", "USER", String.valueOf(created.getId()),
                "username=" + created.getUsername() + ",branchId=" + branchId);
        return toItem(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public BranchReceptionistItemResponse update(@PathVariable Integer id, @Valid @RequestBody BranchReceptionistUpsertRequest req, Authentication authentication) {
        Integer branchId = resolveBranchId(authentication);
        String u = authentication != null ? authentication.getName() : "unknown";

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        if (target.getRole() != UserRole.RECEPTIONIST) throw new RuntimeException("Chỉ cho phép quản lí tài khoản lễ tân.");
        if (target.getBranch() == null || target.getBranch().getId() == null || !target.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Không có quyền sửa lễ tân của chi nhánh khác.");
        }

        // Username is globally unique and should not be changed (BR1).
        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        if (!username.equals(target.getUsername())) {
            throw new RuntimeException("Không cho phép đổi tên đăng nhập.");
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

        // Force: receptionist belongs to one branch only (BR2).
        Hotel branch = hotelRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));
        target.setBranch(branch);
        target.setRole(UserRole.RECEPTIONIST);

        User saved = userRepository.save(target);
        log.info("[BRANCH] update receptionist: user={}, branchId={}, receptionistId={}", u, branchId, id);
        activityLogService.log(authentication, "RECEPTIONIST_UPDATE", "USER", String.valueOf(id),
                "username=" + saved.getUsername() + ",branchId=" + branchId);
        return toItem(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public BranchReceptionistActionResponse revoke(@PathVariable Integer id, Authentication authentication) {
        Integer branchId = resolveBranchId(authentication);
        String u = authentication != null ? authentication.getName() : "unknown";

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        if (target.getRole() != UserRole.RECEPTIONIST) throw new RuntimeException("Chỉ cho phép thu hồi tài khoản lễ tân.");
        if (target.getBranch() == null || target.getBranch().getId() == null || !target.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Không có quyền thu hồi lễ tân của chi nhánh khác.");
        }

        // Revoke permanently: keep username occupied, but invalidate password and remove branch/role.
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        String randomSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        target.setPassword(passwordEncoder.encode(randomSecret));
        target.setRole(UserRole.SYSTEM);
        target.setBranch(null);
        userRepository.save(target);

        log.info("[BRANCH] revoke receptionist: user={}, branchId={}, receptionistId={}, username={}", u, branchId, id, target.getUsername());
        activityLogService.log(authentication, "RECEPTIONIST_REVOKE", "USER", String.valueOf(id),
                "username=" + target.getUsername() + ",branchId=" + branchId);
        return BranchReceptionistActionResponse.builder()
                .id(id)
                .message("Đã thu hồi quyền truy cập vĩnh viễn.")
                .build();
    }
}

