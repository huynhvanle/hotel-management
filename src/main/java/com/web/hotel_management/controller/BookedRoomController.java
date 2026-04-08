package com.web.hotel_management.controller;

import com.web.hotel_management.entity.BookedRoom;
import com.web.hotel_management.service.BookedRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/booked-rooms")
public class BookedRoomController {

    @Autowired
    private BookedRoomService bookedRoomService;

    @GetMapping
    public ResponseEntity<List<BookedRoom>> getAllBookedRooms() {
        return ResponseEntity.ok(bookedRoomService.getAllBookedRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookedRoom> getBookedRoomById(@PathVariable Integer id) {
        BookedRoom bookedRoom = bookedRoomService.getBookedRoomById(id);
        if (bookedRoom != null) {
            return ResponseEntity.ok(bookedRoom);
        }
        return ResponseEntity.notFound().build();
    }
}