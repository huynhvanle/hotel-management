package com.web.hotel_management.booking.service;

import com.web.hotel_management.booking.entity.BookingRoom;
import com.web.hotel_management.room.entity.Room;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class BookingPricingHelper {

    /**
     * Tổng tiền đơn (giá loại phòng × số đêm × số phòng trong đơn).
     */
    public double stayTotal(List<Room> rooms, LocalDate checkin, LocalDate checkout) {
        if (rooms == null || rooms.isEmpty() || checkin == null || checkout == null) {
            return 0d;
        }
        if (!checkout.isAfter(checkin)) {
            return 0d;
        }
        long nights = ChronoUnit.DAYS.between(checkin, checkout);
        double sumPerNight = 0d;
        for (Room r : rooms) {
            if (r == null) continue;
            Double per = r.getRoomType() != null ? r.getRoomType().getBasePrice() : null;
            sumPerNight += per != null ? per : 0d;
        }
        return sumPerNight * nights;
    }

    public double stayTotalFromBookingRooms(List<BookingRoom> bookingRooms, LocalDate checkin, LocalDate checkout) {
        if (bookingRooms == null || bookingRooms.isEmpty()) {
            return 0d;
        }
        if (checkin == null || checkout == null || !checkout.isAfter(checkin)) return 0d;
        long nights = ChronoUnit.DAYS.between(checkin, checkout);
        double sumPerNight = 0d;
        for (BookingRoom br : bookingRooms) {
            if (br == null) continue;
            Double per = br.getUnitPrice();
            if (per == null) {
                Room r = br.getRoom();
                per = r != null && r.getRoomType() != null ? r.getRoomType().getBasePrice() : null;
            }
            sumPerNight += per != null ? per : 0d;
        }
        return sumPerNight * nights;
    }

    /** Tiền cọc quy ước: 50% tổng đơn (mọi kênh đặt). */
    public double depositFromTotal(double orderTotal) {
        return orderTotal * 0.5d;
    }
}
