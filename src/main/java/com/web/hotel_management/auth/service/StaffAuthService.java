package com.web.hotel_management.auth.service;

import com.web.hotel_management.auth.dto.AuthResponse;
import com.web.hotel_management.auth.dto.LoginRequest;
import com.web.hotel_management.auth.security.JwtTokenProvider;
import com.web.hotel_management.user.dto.UserDTO;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Sai tên đăng nhập hoặc mật khẩu."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(UserDTO.fromEntity(user))
                .build();
    }
}

