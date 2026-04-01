package com.web.hotel_management.auth.service;

import com.web.hotel_management.auth.dto.AuthResponse;
import com.web.hotel_management.auth.dto.LoginRequest;
import com.web.hotel_management.auth.dto.RegisterRequest;
import com.web.hotel_management.auth.dto.UserDTO;
import com.web.hotel_management.auth.entity.RefreshToken;
import com.web.hotel_management.auth.entity.User;
import com.web.hotel_management.auth.repository.RefreshTokenRepository;
import com.web.hotel_management.auth.repository.UserRepository;
import com.web.hotel_management.auth.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
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
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setIdCardNumber(request.getIdCardNumber());
        user.setAddress(request.getAddress());
        user.setRole(User.Role.CLIENT);

        user = userRepository.save(user);

        return generateAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return generateAuthResponse(user);
    }

    public AuthResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isExpired() || refreshToken.getRevoked()) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername());

        UserDTO userDTO = UserDTO.fromEntity(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000; // Convert to seconds

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userDTO)
                .build();
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshTokenString = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // Save refresh token to database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenString)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        UserDTO userDTO = UserDTO.fromEntity(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000; // Convert to seconds

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userDTO)
                .build();
    }
}
