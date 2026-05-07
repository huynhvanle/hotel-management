package com.web.hotel_management.reception.controller;

import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.reception.dto.ReceptionClientSearchItem;
import com.web.hotel_management.reception.dto.ReceptionClientCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reception/clients")
@RequiredArgsConstructor
public class ReceptionClientController {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    @GetMapping("/search")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public List<ReceptionClientSearchItem> search(
            @RequestParam(name = "idCardNumber", required = false) String idCardNumberRaw,
            @RequestParam(name = "phone", required = false) String phone,
            Authentication authentication
    ) {
        String u = authentication != null ? authentication.getName() : "unknown";
        Long idCardNumber = null;
        if (idCardNumberRaw != null && !idCardNumberRaw.trim().isBlank()) {
            String s = idCardNumberRaw.trim().replaceAll("\\s+", "");
            if (!s.matches("\\d+")) throw new RuntimeException("CCCD/Passport phải là số.");
            try {
                idCardNumber = Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new RuntimeException("CCCD/Passport không hợp lệ.");
            }
        }
        log.info("[RECEPTION] search clients: user={}, idCardNumber={}, phone={}", u, idCardNumber, phone);
        activityLogService.log(authentication, "CLIENT_SEARCH", "CLIENT", null,
                "idCardNumber=" + idCardNumber + ",phone=" + phone);
        return clientRepository.searchByIdCardOrPhone(idCardNumber, phone).stream()
                .map(c -> ReceptionClientSearchItem.builder()
                        .id(c.getId())
                        .fullName(c.getFullName())
                        .phone(c.getPhone())
                        .idCardNumber(c.getIdCardNumber())
                        .build())
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionClientSearchItem detail(@PathVariable Integer id, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] view client detail: user={}, clientId={}", u, id);
        activityLogService.log(authentication, "CLIENT_DETAIL_VIEW", "CLIENT", String.valueOf(id), null);
        Client c = clientRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng."));
        return ReceptionClientSearchItem.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .idCardNumber(c.getIdCardNumber())
                .build();
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionClientSearchItem create(@Valid @RequestBody ReceptionClientCreateRequest req, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";

        String phone = req.getPhone() == null ? "" : req.getPhone().trim();
        String fullName = req.getFullName() == null ? "" : req.getFullName().trim();
        String idRaw = req.getIdCardNumber() == null ? "" : req.getIdCardNumber().trim().replaceAll("\\s+", "");
        Long idCardNumber;
        try {
            idCardNumber = Long.parseLong(idRaw);
        } catch (NumberFormatException e) {
            throw new RuntimeException("CCCD/Passport không hợp lệ.");
        }

        // Pre-check to return friendly message (DB still enforces uniqueness).
        if (clientRepository.existsByPhone(phone)) throw new RuntimeException("Số điện thoại đã tồn tại.");
        if (clientRepository.existsByIdCardNumber(idCardNumber)) throw new RuntimeException("CCCD/Passport đã tồn tại.");

        log.info("[RECEPTION] create client: user={}, phone={}, idCardNumber={}", u, phone, idCardNumber);
        try {
            Client saved = clientRepository.save(Client.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .idCardNumber(idCardNumber)
                    // passwordHash is required by schema; create a random-ish placeholder.
                    .passwordHash(passwordEncoder.encode("temp-" + phone))
                    .address(null)
                    .build());
            activityLogService.log(authentication, "CLIENT_CREATE", "CLIENT", String.valueOf(saved.getId()),
                    "phone=" + saved.getPhone() + ",fullName=" + saved.getFullName());
            return ReceptionClientSearchItem.builder()
                    .id(saved.getId())
                    .fullName(saved.getFullName())
                    .phone(saved.getPhone())
                    .idCardNumber(saved.getIdCardNumber())
                    .build();
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            String lower = msg != null ? msg.toLowerCase() : "";
            if (lower.contains("phone") || lower.contains("uk_client_phone")) {
                throw new RuntimeException("Số điện thoại đã tồn tại.");
            }
            if (lower.contains("idcard") || lower.contains("id_card")) {
                throw new RuntimeException("CCCD/Passport đã tồn tại.");
            }
            throw new RuntimeException("Không thể thêm khách hàng: dữ liệu trùng hoặc không hợp lệ.");
        }
    }
}

