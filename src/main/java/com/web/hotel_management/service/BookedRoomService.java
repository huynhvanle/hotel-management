package com.web.hotel_management.service;

import com.web.hotel_management.entity.BookedRoom;
import com.web.hotel_management.repository.BookedRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookedRoomService {

    @Autowired
    private BookedRoomRepository bookedRoomRepository;

    public List<BookedRoom> getAllBookedRooms() {
        return bookedRoomRepository.findAll();
    }

    public List<BookedRoom> getBookedRoomsByBookingId(Integer bookingId) {
        return bookedRoomRepository.findAll(); 
    }

    public BookedRoom getBookedRoomById(Integer id) {
        return bookedRoomRepository.findById(id).orElse(null);
    }
}