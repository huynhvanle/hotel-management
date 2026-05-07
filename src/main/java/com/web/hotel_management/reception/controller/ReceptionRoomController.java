package com.web.hotel_management.reception.controller;

import com.web.hotel_management.reception.dto.ReceptionRoomItemResponse;
import com.web.hotel_management.reception.dto.ReceptionRoomStatusUpdateRequest;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.room.dto.RoomStaffResponse;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.entity.RoomStatus;
import com.web.hotel_management.room.repository.RoomRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reception/rooms")
@RequiredArgsConstructor
public class ReceptionRoomController {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final ActivityLogService activityLogService;

    @GetMapping
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public List<ReceptionRoomItemResponse> list(
            @RequestParam(name = "roomNumber", required = false) String roomNumber,
            @RequestParam(name = "floor", required = false) Integer floor,
            @RequestParam(name = "type", required = false) String type,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) throw new RuntimeException("Thiếu thông tin đăng nhập.");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        Integer hotelId = user.getBranch() != null ? user.getBranch().getId() : null;
        if (hotelId == null) throw new RuntimeException("Tài khoản chưa được gán chi nhánh.");

        String u = authentication.getName();
        log.info("[RECEPTION] list rooms: user={}, hotelId={}, roomNumber={}, type={}", u, hotelId, roomNumber, type);
        activityLogService.log(authentication, "ROOM_LIST", "BRANCH", String.valueOf(hotelId),
                "roomNumber=" + roomNumber + ",floor=" + floor + ",type=" + type);

        String qType = (type == null || type.trim().isEmpty()) ? null : type.trim();
        String qRoom = (roomNumber == null || roomNumber.trim().isEmpty()) ? null : roomNumber.trim();
        if (qRoom != null && !qRoom.matches("\\d+")) {
            throw new RuntimeException("Số phòng chỉ gồm chữ số (vd: 301, 102).");
        }
        Integer qFloor = floor;
        if (qFloor != null && qFloor < 0) {
            throw new RuntimeException("Tầng không hợp lệ.");
        }

        // Get all rooms in branch (and optional type filter in DB), then filter by numeric roomNumber.
        List<RoomStaffResponse> rows = roomRepository.search(hotelId, null, qType, null, null)
                .stream()
                .map(RoomStaffResponse::fromEntity)
                .toList();

        return rows.stream()
                .map(r -> {
                    String rawId = r.getId();
                    String digits = rawId == null ? "" : rawId.replaceAll("\\D+", "");
                    String rn = digits.isEmpty() ? (rawId == null ? "" : rawId) : digits;
                    Integer floorNo = null;
                    if (!rn.isEmpty() && rn.charAt(0) >= '0' && rn.charAt(0) <= '9') {
                        floorNo = Character.getNumericValue(rn.charAt(0));
                    }
                    return ReceptionRoomItemResponse.builder()
                            .roomNumber(rn)
                            .rawId(rawId)
                            .floor(floorNo)
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

    @PutMapping("/{roomId}/status")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionRoomItemResponse updateStatus(
            @PathVariable String roomId,
            @Valid @RequestBody ReceptionRoomStatusUpdateRequest req,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) throw new RuntimeException("Thiếu thông tin đăng nhập.");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
        Integer hotelId = user.getBranch() != null ? user.getBranch().getId() : null;
        if (hotelId == null) throw new RuntimeException("Tài khoản chưa được gán chi nhánh.");

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng."));
        if (room.getHotel() == null || room.getHotel().getId() == null || !room.getHotel().getId().equals(hotelId)) {
            throw new RuntimeException("Không có quyền cập nhật phòng của chi nhánh khác.");
        }

        String raw = req.getStatus() == null ? "" : req.getStatus().trim().toUpperCase();
        RoomStatus st;
        try {
            st = RoomStatus.valueOf(raw);
        } catch (Exception e) {
            throw new RuntimeException("Trạng thái không hợp lệ.");
        }

        // Chỉ cho phép chuyển sang Có sẵn/Bảo trì nếu phòng KHÔNG gắn với đơn đang lưu trú (đã check-in).
        if ((st == RoomStatus.AVAILABLE || st == RoomStatus.MAINTENANCE)
                && bookingRoomRepository.existsActiveStay(room.getId(), LocalDate.now())) {
            throw new RuntimeException("Không thể chuyển trạng thái sang Có sẵn/Bảo trì khi phòng đang trong thời gian lưu trú.");
        }

        log.info("[RECEPTION] update room status: user={}, hotelId={}, roomId={}, status={}", username, hotelId, roomId, st);
        activityLogService.log(authentication, "ROOM_STATUS_UPDATE", "ROOM", String.valueOf(roomId),
                "hotelId=" + hotelId + ",status=" + st.name());
        room.setStatus(st);
        Room saved = roomRepository.save(room);

        String digits = saved.getId() == null ? "" : saved.getId().replaceAll("\\D+", "");
        String rn = digits.isEmpty() ? (saved.getId() == null ? "" : saved.getId()) : digits;
        Integer floorNo = null;
        if (!rn.isEmpty() && rn.charAt(0) >= '0' && rn.charAt(0) <= '9') {
            floorNo = Character.getNumericValue(rn.charAt(0));
        }
        return ReceptionRoomItemResponse.builder()
                .roomNumber(rn)
                .rawId(saved.getId())
                .floor(floorNo)
                .status(saved.getStatus())
                .roomTypeName(saved.getRoomType() != null ? saved.getRoomType().getName() : null)
                .price(saved.getRoomType() != null ? saved.getRoomType().getBasePrice() : null)
                .hotelId(saved.getHotel() != null ? saved.getHotel().getId() : null)
                .hotelName(saved.getHotel() != null ? saved.getHotel().getName() : null)
                .build();
    }
}

