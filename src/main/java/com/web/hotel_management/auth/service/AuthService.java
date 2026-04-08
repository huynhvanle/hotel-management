package com.web.hotel_management.auth.service;

import com.web.hotel_management.auth.dto.AuthResponse;
import com.web.hotel_management.auth.dto.LoginRequest;
import com.web.hotel_management.auth.dto.RegisterRequest;
import com.web.hotel_management.auth.entity.RefreshToken;
import com.web.hotel_management.auth.security.JwtTokenProvider;
import com.web.hotel_management.user.dto.UserDTO;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.auth.repository.RefreshTokenRepository;
import com.web.hotel_management.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        if (userRepository.existsByMail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        String position = request.getPosition() != null && !request.getPosition().isBlank()
                ? request.getPosition().trim()
                : "RECEPTIONIST";

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .position(position)
                .mail(request.getEmail())
                .description(request.getDescription())
                .build();

        user = userRepository.save(user);

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return generateAuthResponse(user);
    }

    public AuthResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isExpired() || refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername());

        UserDTO userDTO = UserDTO.fromEntity(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userDTO)
                .build();
    }

    public void logout(Integer userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        UserDTO userDTO = UserDTO.fromEntity(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userDTO)
                .build();
    }
}
