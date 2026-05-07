package com.web.hotel_management.reception.controller;

import com.web.hotel_management.reception.dto.ReceptionRoomItemResponse;
import com.web.hotel_management.reception.dto.ReceptionWalkInBookingRequest;
import com.web.hotel_management.reception.dto.ReceptionWalkInBookingResponse;
import com.web.hotel_management.reception.service.ReceptionWalkInService;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reception/walk-in")
@RequiredArgsConstructor
public class ReceptionWalkInController {

    private final UserRepository userRepository;
    private final ReceptionWalkInService receptionWalkInService;
    private final ActivityLogService activityLogService;

    private Integer resolveHotelId(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) throw new RuntimeException("Thiếu thông tin đăng nhập.");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        Integer hotelId = user.getBranch() != null ? user.getBranch().getId() : null;
        if (hotelId == null) throw new RuntimeException("Tài khoản chưa được gán chi nhánh.");
        return hotelId;
    }

    @GetMapping("/available-rooms")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public List<ReceptionRoomItemResponse> availableRooms(
            @RequestParam LocalDate checkin,
            @RequestParam LocalDate checkout,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "floor", required = false) Integer floor,
            @RequestParam(name = "roomNumber", required = false) String roomNumber,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            Authentication authentication
    ) {
        Integer hotelId = resolveHotelId(authentication);
        String u = authentication.getName();
        log.info("[RECEPTION] walk-in search rooms: user={}, hotelId={}, checkin={}, checkout={}, type={}, floor={}, roomNumber={}, minPrice={}, maxPrice={}",
                u, hotelId, checkin, checkout, type, floor, roomNumber, minPrice, maxPrice);
        activityLogService.log(authentication, "WALKIN_AVAILABLE_ROOMS_SEARCH", "BRANCH", String.valueOf(hotelId),
                "checkin=" + checkin + ",checkout=" + checkout + ",type=" + type + ",floor=" + floor + ",roomNumber=" + roomNumber);
        return receptionWalkInService.searchAvailableRooms(hotelId, checkin, checkout, type, floor, roomNumber, minPrice, maxPrice);
    }

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionWalkInBookingResponse createBooking(
            @Valid @RequestBody ReceptionWalkInBookingRequest req,
            Authentication authentication
    ) {
        Integer hotelId = resolveHotelId(authentication);
        String u = authentication.getName();
        log.info("[RECEPTION] walk-in create booking: user={}, hotelId={}, clientId={}, checkin={}, checkout={}, roomIds={}",
                u, hotelId, req.getClientId(), req.getCheckin(), req.getCheckout(), req.getRoomIds());
        ReceptionWalkInBookingResponse res = receptionWalkInService.createWalkInBooking(
                hotelId,
                req.getClientId(),
                req.getCheckin(),
                req.getCheckout(),
                req.getRoomIds()
        );
        activityLogService.log(authentication, "WALKIN_BOOKING_CREATE", "BOOKING",
                res != null ? String.valueOf(res.getBookingId()) : null,
                "hotelId=" + hotelId + ",clientId=" + req.getClientId() + ",roomIds=" + String.valueOf(req.getRoomIds()));
        return res;
    }
}
