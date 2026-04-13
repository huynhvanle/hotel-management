package com.web.hotel_management.booking.controller;

import com.web.hotel_management.booking.dto.BookingResponse;
import com.web.hotel_management.booking.dto.CreateBookingRequest;
import com.web.hotel_management.booking.service.PublicBookingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final PublicBookingService publicBookingService;

    public BookingController(PublicBookingService publicBookingService) {
        this.publicBookingService = publicBookingService;
    }

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return publicBookingService.createPublicBooking(request);
    }
}
