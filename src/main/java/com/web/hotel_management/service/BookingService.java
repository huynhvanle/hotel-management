package com.web.hotel_management.service;

import com.web.hotel_management.dto.BookingRequest;
import com.web.hotel_management.dto.BookingResponse;
import com.web.hotel_management.dto.RoomResponse;
import com.web.hotel_management.entity.*;
import com.web.hotel_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookedRoomRepository bookedRoomRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Booking booking = new Booking();
        booking.setBookingDate(LocalDateTime.now());
        booking.setCustomerName(request.getCustomerName());
        
        Booking savedBooking = bookingRepository.save(booking);

        List<BookedRoom> bookedRooms = new ArrayList<>();
        double totalAmount = 0;

        LocalDate start = LocalDate.parse(request.getCheckInDate());
        LocalDate end = LocalDate.parse(request.getCheckOutDate());
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;

        for (Integer roomId : request.getRoomIds()) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));

            if ("BOOKED".equals(room.getStatus())) {
                throw new RuntimeException("Room " + roomId + " is already booked");
            }

            room.setStatus("BOOKED");
            roomRepository.save(room);

            BookedRoom bookedRoom = new BookedRoom();
            bookedRoom.setRoom(room);
            bookedRoom.setBooking(savedBooking);
            bookedRoom.setPrice(room.getPrice());
            
            bookedRoomRepository.save(bookedRoom);
            bookedRooms.add(bookedRoom);

            totalAmount += room.getPrice() * days;
        }

        savedBooking.setBookedRooms(bookedRooms);
        savedBooking.setTotalAmount(totalAmount);
        bookingRepository.save(savedBooking);

        BookingResponse response = new BookingResponse();
        response.setBookingId(savedBooking.getId());
        response.setCustomerName(savedBooking.getCustomerName());
        response.setTotalAmount(savedBooking.getTotalAmount());
        response.setBookingDate(savedBooking.getBookingDate());
        response.setStatus("SUCCESS");

        List<RoomResponse> roomResponses = bookedRooms.stream().map(br -> {
            RoomResponse rr = new RoomResponse();
            rr.setId(br.getRoom().getId());
            rr.setType(br.getRoom().getType());
            rr.setPrice(br.getPrice()); 
            if (br.getRoom().getHotel() != null) {
                rr.setHotelName(br.getRoom().getHotel().getName());
            }
            return rr;
        }).toList();

        response.setBookedRooms(roomResponses);

        return response;
    }
}