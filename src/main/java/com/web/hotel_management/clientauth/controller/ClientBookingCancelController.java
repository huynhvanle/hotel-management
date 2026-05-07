package com.web.hotel_management.clientauth.controller;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingRoom;
import com.web.hotel_management.booking.entity.BookingStatus;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.clientauth.dto.ClientCancelBookingResponse;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.entity.RoomStatus;
import com.web.hotel_management.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientBookingCancelController {

    private final ClientRepository clientRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final RoomRepository roomRepository;
    private final ActivityLogService activityLogService;

    @PutMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientCancelBookingResponse cancel(@PathVariable Integer bookingId, Authentication authentication) {
        String phone = authentication != null ? authentication.getName() : null;
        if (phone == null || phone.isBlank()) throw new RuntimeException("Invalid client identity.");
        activityLogService.log(authentication, "CLIENT_BOOKING_CANCEL", "BOOKING", String.valueOf(bookingId), null);
        Client client = clientRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new RuntimeException("Client not found."));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found."));
        if (booking.getClient() == null || booking.getClient().getId() == null || !booking.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Bạn không có quyền huỷ đơn này.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return ClientCancelBookingResponse.builder()
                    .bookingId(booking.getId())
                    .status(BookingStatus.CANCELLED)
                    .message("Đơn đã được huỷ trước đó.")
                    .build();
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            releaseRooms(bookingId);
            return ClientCancelBookingResponse.builder()
                    .bookingId(booking.getId())
                    .status(BookingStatus.CANCELLED)
                    .message("Đã huỷ đơn (chờ xác nhận).")
                    .build();
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Không thể huỷ đơn ở trạng thái hiện tại.");
        }

        LocalDate checkin = booking.getCheckin();
        if (checkin == null) throw new RuntimeException("Thiếu ngày nhận phòng.");
        LocalDate today = LocalDate.now();
        long hoursToCheckin = ChronoUnit.HOURS.between(today.atStartOfDay(), checkin.atStartOfDay());

        // Rule: confirmed booking can cancel only if > 48h (>=2 days) before checkin.
        if (hoursToCheckin < 48) {
            throw new RuntimeException("Không thể huỷ trong vòng 48h trước ngày nhận phòng. Bạn sẽ mất toàn bộ tiền cọc.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        releaseRooms(bookingId);
        return ClientCancelBookingResponse.builder()
                .bookingId(booking.getId())
                .status(BookingStatus.CANCELLED)
                .message("Đã huỷ đơn. Vui lòng liên hệ khách sạn để được hoàn tiền cọc (nếu áp dụng).")
                .build();
    }

    private void releaseRooms(Integer bookingId) {
        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(bookingId);
        if (brs.isEmpty()) return;
        List<String> roomIds = brs.stream()
                .map(br -> br.getRoom() != null ? br.getRoom().getId() : null)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        if (roomIds.isEmpty()) return;
        List<Room> rooms = roomRepository.findAllById(roomIds);
        for (Room r : rooms) {
            // Only flip back if room was made unavailable by bookings.
            if (r.getStatus() == RoomStatus.UNAVAILABLE) {
                r.setStatus(RoomStatus.AVAILABLE);
            }
        }
        roomRepository.saveAll(rooms);
    }
}

