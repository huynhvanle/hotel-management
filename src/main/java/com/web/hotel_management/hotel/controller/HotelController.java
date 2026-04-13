package com.web.hotel_management.hotel.controller;

import com.web.hotel_management.hotel.dto.HotelResponse;
import com.web.hotel_management.hotel.repository.HotelRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    private final HotelRepository hotelRepository;

    public HotelController(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @GetMapping
    public List<HotelResponse> listHotels() {
        return hotelRepository.findAll().stream().map(HotelResponse::fromEntity).toList();
    }
}
