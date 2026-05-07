package com.web.hotel_management.room.controller;

import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.room.dto.RoomStaffRequest;
import com.web.hotel_management.room.dto.RoomStaffResponse;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.entity.RoomStatus;
import com.web.hotel_management.room.repository.RoomRepository;
import com.web.hotel_management.roomtype.entity.RoomType;
import com.web.hotel_management.roomtype.repository.RoomTypeRepository;
import com.web.hotel_management.activity.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RestController
@RequestMapping("/api/branch/rooms")
public class RoomStaffController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final com.web.hotel_management.user.repository.UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public RoomStaffController(
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            BookingRoomRepository bookingRoomRepository,
            RoomTypeRepository roomTypeRepository,
            com.web.hotel_management.user.repository.UserRepository userRepository,
            ActivityLogService activityLogService
    ) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    private Integer resolveHotelId(Authentication authentication, Integer requestedHotelId) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) throw new RuntimeException("Thiếu thông tin đăng nhập.");
        com.web.hotel_management.user.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));

        String role = user.getRole() != null ? user.getRole().name() : "";
        if ("ADMIN".equals(role)) {
            if (requestedHotelId == null) throw new RuntimeException("Thiếu hotelId.");
            return requestedHotelId;
        }

        Integer hotelId = user.getBranch() != null ? user.getBranch().getId() : null;
        if (hotelId == null) throw new RuntimeException("Tài khoản chưa được gán chi nhánh.");
        return hotelId;
    }

    @GetMapping
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public List<RoomStaffResponse> list(
            @RequestParam(required = false) Integer hotelId,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String type,
            Authentication authentication
    ) {
        Integer resolvedHotelId = resolveHotelId(authentication, hotelId);
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[BRANCH] list rooms: user={}, hotelId={}, roomNumber={}, floor={}, type={}", u, resolvedHotelId, roomNumber, floor, type);
        activityLogService.log(authentication, "BRANCH_ROOM_LIST", "BRANCH", String.valueOf(resolvedHotelId),
                "roomNumber=" + roomNumber + ",floor=" + floor + ",type=" + type);

        String qRoom = normalize(roomNumber);
        if (qRoom != null && !qRoom.matches("\\d+")) {
            throw new RuntimeException("Số phòng chỉ gồm chữ số (vd: 301, 102).");
        }
        Integer qFloor = floor;
        if (qFloor != null && qFloor < 0) {
            throw new RuntimeException("Tầng không hợp lệ.");
        }
        return roomRepository.search(resolvedHotelId, null, normalize(type), null, null)
                .stream()
                .map(RoomStaffResponse::fromEntity)
                .filter(r -> qRoom == null || qRoom.equals(r.getId()))
                .filter(r -> qFloor == null || (r.getFloor() != null && r.getFloor().equals(qFloor)))
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public RoomStaffResponse get(@PathVariable String id, @RequestParam(required = false) Integer hotelId, Authentication authentication) {
        Integer resolvedHotelId = resolveHotelId(authentication, hotelId);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        if (room.getHotel() == null || room.getHotel().getId() == null || !Objects.equals(room.getHotel().getId(), resolvedHotelId)) {
            throw new RuntimeException("Không có quyền xem phòng của chi nhánh khác.");
        }
        return RoomStaffResponse.fromEntity(room);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public RoomStaffResponse create(@Valid @RequestBody RoomStaffRequest req, Authentication authentication) {
        String id = req.getId() != null ? req.getId().trim() : null;
        if (id == null || id.isBlank()) {
            throw new RuntimeException("Số phòng là bắt buộc.");
        }
        if (!id.matches("\\d+")) throw new RuntimeException("Số phòng chỉ gồm chữ số (vd: 301, 102).");
        if (roomRepository.existsById(id)) {
            throw new RuntimeException("Số phòng đã tồn tại.");
        }

        Integer resolvedHotelId = resolveHotelId(authentication, req.getHotelId());
        String u = authentication != null ? authentication.getName() : "unknown";
        Hotel hotel = hotelRepository.findById(resolvedHotelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));

        RoomType rt = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng."));
        Integer floor = req.getFloor();
        if (floor == null || floor < 0) throw new RuntimeException("Tầng không hợp lệ.");

        Room room = Room.builder()
                .id(id)
                .floor(req.getFloor())
                // tạo mới luôn là Có sẵn
                .status(RoomStatus.AVAILABLE)
                .roomType(rt)
                .hotel(hotel)
                .build();
        log.info("[BRANCH] create room: user={}, hotelId={}, roomId={}, roomTypeId={}, floor={}",
                u, resolvedHotelId, id, rt.getId(), floor);
        Room saved = roomRepository.save(room);
        activityLogService.log(authentication, "BRANCH_ROOM_CREATE", "ROOM", String.valueOf(saved.getId()),
                "hotelId=" + resolvedHotelId + ",roomTypeId=" + rt.getId() + ",floor=" + floor);
        return RoomStaffResponse.fromEntity(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public RoomStaffResponse update(@PathVariable String id, @Valid @RequestBody RoomStaffRequest req, Authentication authentication) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng."));

        Integer resolvedHotelId = resolveHotelId(authentication, req.getHotelId());
        String u = authentication != null ? authentication.getName() : "unknown";
        if (room.getHotel() == null || room.getHotel().getId() == null || !Objects.equals(room.getHotel().getId(), resolvedHotelId)) {
            throw new RuntimeException("Không có quyền sửa phòng của chi nhánh khác.");
        }
        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            throw new RuntimeException("Chỉ cho phép sửa phòng khi trạng thái là Bảo trì.");
        }

        Hotel hotel = hotelRepository.findById(req.getHotelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));

        RoomType rt = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng."));
        Integer floor = req.getFloor();
        if (floor == null || floor < 0) throw new RuntimeException("Tầng không hợp lệ.");

        room.setFloor(req.getFloor());
        room.setHotel(hotel);
        room.setRoomType(rt);
        room.setStatus(RoomStatus.MAINTENANCE);

        log.info("[BRANCH] update room: user={}, hotelId={}, roomId={}, roomTypeId={}, floor={}",
                u, resolvedHotelId, id, rt.getId(), floor);
        Room saved = roomRepository.save(room);
        activityLogService.log(authentication, "BRANCH_ROOM_UPDATE", "ROOM", String.valueOf(saved.getId()),
                "hotelId=" + resolvedHotelId + ",roomTypeId=" + rt.getId() + ",floor=" + floor);
        return RoomStaffResponse.fromEntity(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public void delete(@PathVariable String id, @RequestParam(required = false) Integer hotelId, Authentication authentication) {
        Integer resolvedHotelId = resolveHotelId(authentication, hotelId);
        String u = authentication != null ? authentication.getName() : "unknown";
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng."));
        if (room.getHotel() == null || room.getHotel().getId() == null || !Objects.equals(room.getHotel().getId(), resolvedHotelId)) {
            throw new RuntimeException("Không có quyền xoá phòng của chi nhánh khác.");
        }
        if (room.getStatus() != RoomStatus.MAINTENANCE) {
            throw new RuntimeException("Chỉ cho phép xoá phòng khi trạng thái là Bảo trì.");
        }
        if (bookingRoomRepository.existsByRoom_Id(id)) {
            throw new RuntimeException("Không thể xoá phòng vì đã phát sinh đơn đặt.");
        }
        log.info("[BRANCH] delete room: user={}, hotelId={}, roomId={}", u, resolvedHotelId, id);
        activityLogService.log(authentication, "BRANCH_ROOM_DELETE", "ROOM", String.valueOf(id),
                "hotelId=" + resolvedHotelId);
        roomRepository.deleteById(id);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public RoomStaffResponse uploadImage(@PathVariable String id, @RequestPart("file") MultipartFile file, @RequestParam(required = false) Integer hotelId, Authentication authentication) { // id phòng + file ảnh upload
        Integer resolvedHotelId = resolveHotelId(authentication, hotelId);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        if (room.getHotel() == null || room.getHotel().getId() == null || !Objects.equals(room.getHotel().getId(), resolvedHotelId)) {
            throw new RuntimeException("Không có quyền cập nhật phòng của chi nhánh khác.");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        String ct = file.getContentType();
        String ext = contentTypeToExt(ct); // kiểm tra định dạng ảnh map đuôi file
        if (ext == null) {
            throw new RuntimeException("Only JPG/PNG/WEBP/GIF/SVG images are allowed");
        }

        try {
            Path dir = Path.of("uploads", "rooms").toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // xoá cũ
            deleteIfExists(dir.resolve(id + ".jpg"));
            deleteIfExists(dir.resolve(id + ".jpeg"));
            deleteIfExists(dir.resolve(id + ".png"));
            deleteIfExists(dir.resolve(id + ".webp"));
            deleteIfExists(dir.resolve(id + ".gif"));
            deleteIfExists(dir.resolve(id + ".svg"));

            Path out = dir.resolve(id + ext);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }

        activityLogService.log(authentication, "BRANCH_ROOM_IMAGE_UPLOAD", "ROOM", String.valueOf(id),
                "hotelId=" + resolvedHotelId + ",contentType=" + file.getContentType() + ",size=" + file.getSize());
        return RoomStaffResponse.fromEntity(room);
    }

    private static void deleteIfExists(Path p) throws IOException {
        if (Files.exists(p)) Files.delete(p);
    }

    private static String contentTypeToExt(String ct) {
        if (ct == null) return null;
        return switch (ct.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/svg+xml" -> ".svg";
            default -> null;
        };
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Room type is now managed by RoomType entity; legacy normalizer removed.
}

