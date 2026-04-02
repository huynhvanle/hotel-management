package com.web.hotel_management.controller;

import com.web.hotel_management.entity.Booking;
import com.web.hotel_management.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/create")
    public Booking create(@RequestBody Booking booking) {
        return bookingService.createBooking(booking);
    }
}