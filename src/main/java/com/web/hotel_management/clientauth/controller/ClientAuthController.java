package com.web.hotel_management.clientauth.controller;

import com.web.hotel_management.clientauth.dto.ClientAuthResponse;
import com.web.hotel_management.clientauth.dto.ClientLoginRequest;
import com.web.hotel_management.clientauth.dto.ClientRegisterRequest;
import com.web.hotel_management.clientauth.service.ClientAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/client")
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientAuthResponse register(@Valid @RequestBody ClientRegisterRequest req) {
        return clientAuthService.register(req);
    }

    @PostMapping("/login")
    public ResponseEntity<ClientAuthResponse> login(@Valid @RequestBody ClientLoginRequest req) {
        return ResponseEntity.ok(clientAuthService.login(req));
    }
}

