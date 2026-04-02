package com.web.hotel_management.client.controller;

import com.web.hotel_management.client.dto.ClientAuthResponse;
import com.web.hotel_management.client.dto.ClientRegisterRequest;
import com.web.hotel_management.client.service.ClientAuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/hotel-management/client")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class ClientAuthController {

    @Autowired
    private ClientAuthService clientAuthService;

    @PostMapping("/register")
    public ResponseEntity<ClientAuthResponse> register(@Valid @RequestBody ClientRegisterRequest request) {
        try {
            ClientAuthResponse response = clientAuthService.registerClient(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Client registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ClientAuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ClientAuthResponse> getByEmail(@PathVariable String email) {
        try {
            ClientAuthResponse response = clientAuthService.getClientByEmail(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to get client: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ClientAuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientAuthResponse> getById(@PathVariable Integer id) {
        try {
            ClientAuthResponse response = clientAuthService.getClientById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Failed to get client: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ClientAuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientAuthResponse> update(@PathVariable Integer id, @Valid @RequestBody ClientRegisterRequest request) {
        try {
            ClientAuthResponse response = clientAuthService.updateClient(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Client update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ClientAuthResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}
