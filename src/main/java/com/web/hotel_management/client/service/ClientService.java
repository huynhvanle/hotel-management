package com.web.hotel_management.client.service;

import com.web.hotel_management.client.dto.ClientCreateRequest;
import com.web.hotel_management.client.dto.ClientDTO;
import com.web.hotel_management.client.dto.ClientResponse;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientResponse createClient(ClientCreateRequest request) {
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (clientRepository.existsByIdCardNumber(request.getIdCardNumber())) {
            throw new RuntimeException("ID card number already exists");
        }

        Client client = Client.builder()
                .idCardNumber(request.getIdCardNumber())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Client savedClient = clientRepository.save(client);
        log.info("Client created: {}", savedClient.getEmail());

        return ClientResponse.builder()
                .success(true)
                .message("Client created successfully")
                .client(ClientDTO.fromEntity(savedClient))
                .build();
    }

    public ClientResponse getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found with email: " + email));

        return ClientResponse.builder()
                .success(true)
                .message("Client found")
                .client(ClientDTO.fromEntity(client))
                .build();
    }

    public ClientResponse getClientById(@NonNull Integer id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        return ClientResponse.builder()
                .success(true)
                .message("Client found")
                .client(ClientDTO.fromEntity(client))
                .build();
    }

    public ClientResponse updateClient(Integer id, ClientCreateRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        if (!client.getEmail().equals(request.getEmail()) && clientRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        client.setFullName(request.getFullName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        client.setNote(request.getNote());
        client.setUpdatedAt(LocalDateTime.now());

        Client updatedClient = clientRepository.save(client);
        log.info("Client updated: {}", updatedClient.getEmail());

        return ClientResponse.builder()
                .success(true)
                .message("Client updated successfully")
                .client(ClientDTO.fromEntity(updatedClient))
                .build();
    }
}
