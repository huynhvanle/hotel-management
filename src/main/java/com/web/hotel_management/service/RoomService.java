package com.web.hotel_management.service;

import com.web.hotel_management.entity.Room;
import com.web.hotel_management.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public List<Room> getAvailableRooms(Integer hotelId) {
        return roomRepository.findByStatus("AVAILABLE");
    }

    public Room updateRoomPrice(Integer roomId, Double newPrice) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.setPrice(newPrice);
        return roomRepository.save(room);
    }
    //trả về toàn bộ phòng
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    //trả về phòng theo trạng thái
    public List<Room> getRoomsByStatus(String status) {
        return roomRepository.findByStatus(status);
    }
}