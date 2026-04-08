package com.web.hotel_management.service;

import com.web.hotel_management.entity.*;
import com.web.hotel_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceExtraService {

    @Autowired
    private UsedServiceRepository usedServiceRepository;

    @Autowired
    private HotelServiceRepository hotelServiceRepository;

    @Autowired
    private BookedRoomRepository bookedRoomRepository;

    @Transactional
    public void addServiceToRoom(Integer bookedRoomId, Integer serviceId, Integer quantity) {
        // 1. Tìm thông tin phòng đặt
        BookedRoom bookedRoom = bookedRoomRepository.findById(bookedRoomId)
                .orElseThrow(() -> new RuntimeException("Booked Room not found"));

        // 2. Tìm thông tin dịch vụ (Entity HotelService)
        HotelService serviceEntity = hotelServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // 3. Kiểm tra tính hợp lệ: Dịch vụ phải thuộc khách sạn mà khách đang ở
        Integer hotelIdOfRoom = bookedRoom.getRoom().getHotel().getId();
        Integer hotelIdOfService = serviceEntity.getHotel().getId();

        if (!hotelIdOfRoom.equals(hotelIdOfService)) {
            throw new RuntimeException("Dịch vụ này không thuộc về khách sạn hiện tại!");
        }

        // 4. Tạo bản ghi sử dụng dịch vụ bằng Builder
        UsedService usedService = UsedService.builder()
                .bookedRoom(bookedRoom)
                .service(serviceEntity)
                .quantity(quantity)
                .unitPrice(serviceEntity.getPrice())
                .build();

        // 5. Lưu vào Database
        usedServiceRepository.save(usedService);
    }
}