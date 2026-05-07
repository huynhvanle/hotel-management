package com.web.hotel_management.clientauth.controller;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingRoom;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.booking.service.BookingPricingHelper;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.clientauth.dto.ClientBookingItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientBookingController {

    private final ClientRepository clientRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingPricingHelper bookingPricingHelper;
    private final ActivityLogService activityLogService;

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ClientBookingItemResponse> myBookings(Authentication authentication) {
        String phone = authentication != null ? authentication.getName() : null;
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Invalid client identity.");
        }
        activityLogService.log(authentication, "CLIENT_BOOKINGS_VIEW", "CLIENT", null, null);
        Client client = clientRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new RuntimeException("Client not found."));

        List<Booking> bookings = bookingRepository.findByClient_Id(client.getId());
        return bookings.stream()
                .sorted(Comparator.comparing(Booking::getId).reversed())
                .map(b -> {
                    List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
                    boolean checkedIn = b.getCheckedInAt() != null;

                    // Aggregate room types + quantities (always visible).
                    Map<Integer, ClientBookingItemResponse.RoomTypeLine.RoomTypeLineBuilder> roomTypeBuilders = new LinkedHashMap<>();
                    Map<Integer, Integer> roomTypeQty = new LinkedHashMap<>();
                    for (BookingRoom br : brs) {
                        Integer rtId = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getId() : null;
                        String rtName = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getName() : null;
                        if (rtId == null) continue;
                        roomTypeQty.put(rtId, roomTypeQty.getOrDefault(rtId, 0) + 1);
                        if (!roomTypeBuilders.containsKey(rtId)) {
                            roomTypeBuilders.put(rtId, ClientBookingItemResponse.RoomTypeLine.builder()
                                    .roomTypeId(rtId)
                                    .roomTypeName(rtName));
                        }
                    }
                    List<ClientBookingItemResponse.RoomTypeLine> roomTypes = roomTypeQty.entrySet().stream()
                            .map(e -> roomTypeBuilders.get(e.getKey()).quantity(e.getValue()).build())
                            .toList();

                    // Only reveal physical room numbers after check-in.
                    List<String> roomIds = checkedIn
                            ? brs.stream()
                                .map(br -> br.getRoom() != null ? br.getRoom().getId() : null)
                                .filter(x -> x != null && !x.isBlank())
                                .toList()
                            : List.of();

                    double total = bookingPricingHelper.stayTotalFromBookingRooms(brs, b.getCheckin(), b.getCheckout());
                    double deposit = bookingPricingHelper.depositFromTotal(total);

                    return ClientBookingItemResponse.builder()
                            .bookingId(b.getId())
                            .checkin(b.getCheckin())
                            .checkout(b.getCheckout())
                            .status(b.getStatus())
                            .depositAmount(deposit)
                            .totalAmount(total)
                            .checkedIn(checkedIn)
                            .roomTypes(roomTypes)
                            .roomIds(roomIds)
                            .build();
                })
                .toList();
    }
}

