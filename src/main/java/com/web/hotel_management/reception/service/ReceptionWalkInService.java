package com.web.hotel_management.reception.service;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingRoom;
import com.web.hotel_management.booking.entity.BookingStatus;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.reception.dto.ReceptionRoomItemResponse;
import com.web.hotel_management.reception.dto.ReceptionWalkInBookingResponse;
import com.web.hotel_management.room.dto.RoomStaffResponse;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.entity.RoomStatus;
import com.web.hotel_management.room.repository.RoomRepository;
import com.web.hotel_management.booking.service.BookingPricingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReceptionWalkInService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final ClientRepository clientRepository;
    private final BookingPricingHelper bookingPricingHelper;

    public List<ReceptionRoomItemResponse> searchAvailableRooms(
            Integer hotelId,
            LocalDate checkin,
            LocalDate checkout,
            String type,
            Integer floor,
            String roomNumber,
            Double minPrice,
            Double maxPrice
    ) {
        validateDates(checkin, checkout);
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new RuntimeException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }
        String qType = (type == null || type.trim().isEmpty()) ? null : type.trim();
        String qRoom = (roomNumber == null || roomNumber.trim().isEmpty()) ? null : roomNumber.trim();
        if (qRoom != null && !qRoom.matches("\\d+")) {
            throw new RuntimeException("Số phòng chỉ gồm chữ số (vd: 301, 102).");
        }
        Integer qFloor = floor;
        if (qFloor != null && qFloor < 0) {
            throw new RuntimeException("Tầng không hợp lệ.");
        }

        List<RoomStaffResponse> rows = roomRepository
                .searchWalkInAvailable(hotelId, null, qType, minPrice, maxPrice, checkin, checkout)
                .stream()
                .map(RoomStaffResponse::fromEntity)
                .toList();

        return rows.stream()
                .map(r -> {
                    String rawId = r.getId();
                    String digits = rawId == null ? "" : rawId.replaceAll("\\D+", "");
                    String rn = digits.isEmpty() ? (rawId == null ? "" : rawId) : digits;
                    return ReceptionRoomItemResponse.builder()
                            .roomNumber(rn)
                            .rawId(rawId)
                            .floor(r.getFloor())
                            .status(r.getStatus())
                            .roomTypeName(r.getRoomTypeName())
                            .price(r.getPrice())
                            .hotelId(r.getHotelId())
                            .hotelName(r.getHotelName())
                            .build();
                })
                .filter(x -> qRoom == null || qRoom.equals(x.getRoomNumber()))
                .filter(x -> qFloor == null || (x.getFloor() != null && x.getFloor().equals(qFloor)))
                .toList();
    }

    public ReceptionWalkInBookingResponse createWalkInBooking(Integer hotelId, Integer clientId, LocalDate checkin, LocalDate checkout, List<String> roomIdsRaw) {
        validateDates(checkin, checkout);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng."));
        if (client.getIdCardNumber() == null || client.getIdCardNumber() <= 0) {
            throw new RuntimeException("Đặt tại quầy bắt buộc có CCCD/Passport hợp lệ của khách hàng.");
        }

        List<String> roomIds = new LinkedHashSet<>(roomIdsRaw.stream().map(String::trim).filter(s -> !s.isBlank()).toList())
                .stream()
                .toList();
        if (roomIds.isEmpty()) {
            throw new RuntimeException("Chọn ít nhất một phòng.");
        }

        List<Room> rooms = roomRepository.findAllById(roomIds);
        if (rooms.size() != roomIds.size()) {
            throw new RuntimeException("Một hoặc nhiều phòng không tồn tại.");
        }

        for (Room room : rooms) {
            if (room.getHotel() == null || room.getHotel().getId() == null || !room.getHotel().getId().equals(hotelId)) {
                throw new RuntimeException("Phòng không thuộc chi nhánh của bạn: " + room.getId());
            }
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new RuntimeException("Phòng không ở trạng thái sẵn sàng: " + room.getId());
            }
            if (bookingRoomRepository.existsOverlappingActiveBooking(room.getId(), checkin, checkout)) {
                throw new RuntimeException("Phòng đã có đặt trong khoảng ngày đã chọn: " + room.getId());
            }
        }

        double orderTotal = bookingPricingHelper.stayTotal(rooms, checkin, checkout);
        double deposit = bookingPricingHelper.depositFromTotal(orderTotal);

        Booking booking = Booking.builder()
                .checkin(checkin)
                .checkout(checkout)
                .status(BookingStatus.CONFIRMED)
                .depositAmount(deposit)
                .client(client)
                .build();
        Booking saved = bookingRepository.save(booking);

        for (Room room : rooms) {
            bookingRoomRepository.save(BookingRoom.builder()
                    .booking(saved)
                    .room(room)
                    .unitPrice(room.getRoomType() != null ? room.getRoomType().getBasePrice() : null)
                    .build());
        }

        log.info("[RECEPTION] walk-in booking saved: bookingId={}, hotelId={}, clientId={}, rooms={}",
                saved.getId(), hotelId, clientId, roomIds);

        return ReceptionWalkInBookingResponse.builder()
                .bookingId(saved.getId())
                .message("Đặt phòng tại quầy thành công. Mã đơn: " + saved.getId() + ".")
                .build();
    }

    private static void validateDates(LocalDate checkin, LocalDate checkout) {
        if (checkin == null || checkout == null) {
            throw new RuntimeException("Ngày đến và ngày đi là bắt buộc.");
        }
        if (!checkout.isAfter(checkin)) {
            throw new RuntimeException("Ngày đi phải sau ngày đến.");
        }
    }
}
