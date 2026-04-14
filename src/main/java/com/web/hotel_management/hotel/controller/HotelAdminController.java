package com.web.hotel_management.hotel.controller;

import com.web.hotel_management.hotel.dto.HotelAdminRequest;
import com.web.hotel_management.hotel.dto.HotelAdminResponse;
import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/hotels")
public class HotelAdminController {

    private final HotelRepository hotelRepository;

    public HotelAdminController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @GetMapping
    public List<HotelAdminResponse> list() {
        return hotelRepository.findAll().stream().map(HotelAdminResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public HotelAdminResponse get(@PathVariable Integer id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
        return HotelAdminResponse.fromEntity(hotel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelAdminResponse create(@Valid @RequestBody HotelAdminRequest req) {
        Hotel hotel = Hotel.builder()
                .name(req.getName().trim())
                .starLevel(req.getStarLevel())
                .address(req.getAddress())
                .description(req.getDescription())
                .build();
        return HotelAdminResponse.fromEntity(hotelRepository.save(hotel));
    }

    @PutMapping("/{id}")
    public HotelAdminResponse update(@PathVariable Integer id, @Valid @RequestBody HotelAdminRequest req) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
        hotel.setName(req.getName().trim());
        hotel.setStarLevel(req.getStarLevel());
        hotel.setAddress(req.getAddress());
        hotel.setDescription(req.getDescription());
        return HotelAdminResponse.fromEntity(hotelRepository.save(hotel));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!hotelRepository.existsById(id)) {
            throw new RuntimeException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
    }
}

