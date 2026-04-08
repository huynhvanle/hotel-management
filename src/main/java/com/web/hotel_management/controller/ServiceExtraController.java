package com.web.hotel_management.controller;

import com.web.hotel_management.dto.ServiceRequest;
import com.web.hotel_management.service.ServiceExtraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
public class ServiceExtraController {

    @Autowired
    private ServiceExtraService serviceExtraService;

    @PostMapping("/add-to-room")
    public ResponseEntity<String> addService(@RequestBody ServiceRequest request) {
        serviceExtraService.addServiceToRoom(
            request.getBookedRoomId(), 
            request.getServiceId(), 
            request.getQuantity()
        );
        return ResponseEntity.ok("Service added to room successfully");
    }
}