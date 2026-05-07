package com.web.hotel_management.roomtype.controller;

import com.web.hotel_management.roomtype.dto.RoomTypeDto;
import com.web.hotel_management.roomtype.repository.RoomTypeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {
    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeController(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @GetMapping
    public List<RoomTypeDto> list() {
        return roomTypeRepository.findAll().stream()
                .map(RoomTypeDto::fromEntity)
                .toList();
    }
}

