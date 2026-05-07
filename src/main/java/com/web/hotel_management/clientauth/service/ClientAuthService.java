package com.web.hotel_management.clientauth.service;

import com.web.hotel_management.auth.security.JwtTokenProvider;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.clientauth.dto.ClientAuthResponse;
import com.web.hotel_management.clientauth.dto.ClientLoginRequest;
import com.web.hotel_management.clientauth.dto.ClientProfileResponse;
import com.web.hotel_management.clientauth.dto.ClientRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientAuthService {

    public static final String CLIENT_SUBJECT_PREFIX = "client:";

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public ClientAuthResponse register(ClientRegisterRequest req) {
        String phone = (req.getPhone() == null ? "" : req.getPhone().trim());
        if (phone.isBlank()) throw new RuntimeException("Số điện thoại là bắt buộc");
        if (!phone.matches("\\d{10}")) throw new RuntimeException("Số điện thoại phải gồm đúng 10 chữ số");
        if (clientRepository.existsByPhone(phone)) {
            throw new RuntimeException("Số điện thoại đã được đăng ký");
        }
        if (req.getIdCardNumber() != null && clientRepository.existsByIdCardNumber(req.getIdCardNumber())) {
            throw new RuntimeException("Số CCCD/CMND đã được đăng ký");
        }

        Client client;
        try {
            client = clientRepository.save(Client.builder()
                    .phone(phone)
                    .passwordHash(passwordEncoder.encode(req.getPassword()))
                    .fullName(req.getFullName().trim())
                    .address(null)
                    .idCardNumber(req.getIdCardNumber())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            String lower = msg != null ? msg.toLowerCase() : "";
            if (lower.contains("phone") || lower.contains("uk_client_phone")) {
                throw new RuntimeException("Số điện thoại đã được đăng ký");
            }
            if (lower.contains("idcard") || lower.contains("id_card")) {
                throw new RuntimeException("Số CCCD/CMND đã được đăng ký");
            }
            throw new RuntimeException("Không thể đăng ký: dữ liệu trùng hoặc không hợp lệ.");
        }

        return buildAuthResponse(client);
    }

    public ClientAuthResponse login(ClientLoginRequest req) {
        String phone = (req.getPhone() == null ? "" : req.getPhone().trim());
        if (!phone.isBlank() && !phone.matches("\\d{10}")) {
            throw new RuntimeException("Số điện thoại phải gồm đúng 10 chữ số");
        }
        Client client = clientRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Invalid phone or password"));

        if (!passwordEncoder.matches(req.getPassword(), client.getPasswordHash())) {
            throw new RuntimeException("Invalid phone or password");
        }

        return buildAuthResponse(client);
    }

    private ClientAuthResponse buildAuthResponse(Client client) {
        String subject = CLIENT_SUBJECT_PREFIX + client.getPhone();
        String accessToken = jwtTokenProvider.generateAccessToken(subject);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;
        return ClientAuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .client(ClientProfileResponse.fromEntity(client))
                .build();
    }
}

