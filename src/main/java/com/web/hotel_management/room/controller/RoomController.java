package com.web.hotel_management.room.controller;

import com.web.hotel_management.room.dto.RoomResponse;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.repository.RoomRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public List<RoomResponse> listRooms(
            @RequestParam(required = false) Integer hotelId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {
        return roomRepository.search(hotelId, normalize(type), minPrice, maxPrice)
                .stream()
                .map(RoomResponse::fromEntity)
                .toList();
    }

    @GetMapping("/available")
    public List<RoomResponse> listAvailableRooms(
            @RequestParam LocalDate checkin,
            @RequestParam LocalDate checkout,
            @RequestParam(required = false) Integer hotelId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {
        if (checkout.isBefore(checkin) || checkout.isEqual(checkin)) {
            throw new RuntimeException("Checkout must be after checkin");
        }
        return roomRepository.searchAvailable(hotelId, normalize(type), minPrice, maxPrice, checkin, checkout)
                .stream()
                .map(RoomResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public RoomResponse getRoom(@PathVariable String id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        return RoomResponse.fromEntity(room);
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
