package com.web.hotel_management.client.service;

import com.web.hotel_management.client.dto.ClientAuthResponse;
import com.web.hotel_management.client.dto.ClientDTO;
import com.web.hotel_management.client.dto.ClientRegisterRequest;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class ClientAuthService {

    @Autowired
    private ClientRepository clientRepository;

    public ClientAuthResponse registerClient(ClientRegisterRequest request) {
        // Check if email already exists
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Check if ID card number already exists
        if (clientRepository.existsByIdCardNumber(request.getIdCardNumber())) {
            throw new RuntimeException("ID card number is already registered");
        }

        // Create new client
        Client client = Client.builder()
                .idCardNumber(request.getIdCardNumber())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        client = clientRepository.save(client);
        log.info("Client registered successfully: {}", client.getEmail());

        return ClientAuthResponse.builder()
                .success(true)
                .message("Client registered successfully")
                .client(ClientDTO.fromEntity(client))
                .build();
    }

    public ClientAuthResponse getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found with email: " + email));

        return ClientAuthResponse.builder()
                .success(true)
                .message("Client found")
                .client(ClientDTO.fromEntity(client))
                .build();
    }

    public ClientAuthResponse getClientById(Integer id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        return ClientAuthResponse.builder()
                .success(true)
                .message("Client found")
                .client(ClientDTO.fromEntity(client))
                .build();
    }

    public ClientAuthResponse updateClient(Integer id, ClientRegisterRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        // Check if new email is already used
        if (!client.getEmail().equals(request.getEmail()) && clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        client.setFullName(request.getFullName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        client.setDescription(request.getDescription());
        client.setUpdatedAt(LocalDateTime.now());

        client = clientRepository.save(client);
        log.info("Client updated successfully: {}", client.getEmail());

        return ClientAuthResponse.builder()
                .success(true)
                .message("Client updated successfully")
                .client(ClientDTO.fromEntity(client))
                .build();
    }
}
