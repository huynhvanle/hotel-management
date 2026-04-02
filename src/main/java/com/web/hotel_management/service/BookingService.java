package com.web.hotel_management.service;

import com.web.hotel_management.entity.*;
import com.web.hotel_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Transactional // Duy Giang nhớ thêm @Transactional để đảm bảo nếu lỗi thì nó ko lưu gì cả
    public Booking createBooking(Booking booking) {
        double total = 0;
        booking.setBookingDate(LocalDateTime.now());

        for (BookedRoom br : booking.getBookedRooms()) {
            // 1. Tìm phòng "xịn" từ Database dựa trên ID gửi lên
            Room room = roomRepository.findById(br.getRoom().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng có ID: " + br.getRoom().getId()));

            // 2. Gán lại phòng đã tìm thấy vào BookedRoom 
            br.setRoom(room);
            br.setBooking(booking);

            // 3. Tự động đổi trạng thái phòng sang 'BOOKED'
            room.setStatus("BOOKED");
            roomRepository.save(room); // Lưu trạng thái mới vào bảng Room

            // 4. Tính toán tiền dựa trên giá "xịn" trong Database
            long days = ChronoUnit.DAYS.between(br.getCheckin(), br.getCheckout());
            if (days <= 0) days = 1; // Nếu đặt trong ngày thì tính 1 ngày
            total += days * room.getPrice();
        }

        booking.setTotalAmount(total);
        return bookingRepository.save(booking);
    }
}