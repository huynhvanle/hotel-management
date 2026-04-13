package com.web.hotel_management.booking.controller;

import com.web.hotel_management.booking.dto.BookedRoomCheckinRequest;
import com.web.hotel_management.booking.dto.BookedRoomStaffResponse;
import com.web.hotel_management.booking.dto.BookingStaffResponse;
import com.web.hotel_management.booking.entity.BookedRoom;
import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.repository.BookedRoomRepository;
import com.web.hotel_management.booking.repository.BookingRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/bookings")
public class BookingStaffController {

    private final BookingRepository bookingRepository;
    private final BookedRoomRepository bookedRoomRepository;

    public BookingStaffController(BookingRepository bookingRepository, BookedRoomRepository bookedRoomRepository) {
        this.bookingRepository = bookingRepository;
        this.bookedRoomRepository = bookedRoomRepository;
    }

    @GetMapping
    public List<BookingStaffResponse> list(
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer employeeId
    ) {
        List<Booking> bookings;
        if (clientId != null) bookings = bookingRepository.findByClient_Id(clientId);
        else if (employeeId != null) bookings = bookingRepository.findByEmployee_Id(employeeId);
        else bookings = bookingRepository.findAll();

        return bookings.stream().map(b -> {
            BookingStaffResponse dto = BookingStaffResponse.fromEntity(b);
            int count = bookedRoomRepository.findByBooking_Id(b.getId()).size();
            dto.setRoomsCount(count);
            return dto;
        }).toList();
    }

    @GetMapping("/{id}")
    public BookingStaffResponse detail(@PathVariable Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
        BookingStaffResponse dto = BookingStaffResponse.fromEntity(booking);
        List<BookedRoomStaffResponse> rooms = bookedRoomRepository.findByBooking_Id(id).stream()
                .map(BookedRoomStaffResponse::fromEntity)
                .toList();
        dto.setBookedRooms(rooms);
        dto.setRoomsCount(rooms.size());
        return dto;
    }

    @PutMapping("/booked-rooms/{bookedRoomId}/checkin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setCheckin(@PathVariable Integer bookedRoomId, @Valid @RequestBody BookedRoomCheckinRequest req) {
        BookedRoom br = bookedRoomRepository.findById(bookedRoomId)
                .orElseThrow(() -> new RuntimeException("BookedRoom not found with id: " + bookedRoomId));
        int v = req.getIsCheckedIn() != null && req.getIsCheckedIn() == 1 ? 1 : 0;
        br.setIsCheckedIn(v);
        bookedRoomRepository.save(br);
    }
}

