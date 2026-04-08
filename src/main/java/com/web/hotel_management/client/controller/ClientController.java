package com.web.hotel_management.client.controller;

import com.web.hotel_management.client.dto.ClientCreateRequest;
import com.web.hotel_management.client.dto.ClientResponse;
import com.web.hotel_management.client.service.ClientService;
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
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping("/create")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientCreateRequest request) {
        try {
            ClientResponse response = clientService.createClient(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Create client failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ClientResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ClientResponse> getByEmail(@PathVariable String email) {
        try {
            ClientResponse response = clientService.getClientByEmail(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Get client by email failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ClientResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getById(@PathVariable Integer id) {
        try {
            ClientResponse response = clientService.getClientById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Get client by id failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ClientResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ClientResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody ClientCreateRequest request) {
        try {
            ClientResponse response = clientService.updateClient(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Update client failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ClientResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}