package com.web.hotel_management.hotel.controller;

import com.web.hotel_management.hotel.dto.HotelAdminRequest;
import com.web.hotel_management.hotel.dto.HotelAdminResponse;
import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import com.web.hotel_management.user.repository.UserRepository;
import com.web.hotel_management.room.repository.RoomRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.invoice.repository.InvoiceRepository;
import com.web.hotel_management.activity.service.ActivityLogService;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/hotels")
@PreAuthorize("hasRole('ADMIN')")
public class HotelAdminController {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    public HotelAdminController(
            HotelRepository hotelRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoomRepository roomRepository,
            BookingRoomRepository bookingRoomRepository,
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository,
            ActivityLogService activityLogService
    ) {
        this.hotelRepository = hotelRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roomRepository = roomRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public List<HotelAdminResponse> list() {
        return hotelRepository.findAll().stream().map(HotelAdminResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public HotelAdminResponse get(@PathVariable Integer id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
        return HotelAdminResponse.fromEntity(hotel);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public HotelAdminResponse create(@Valid @RequestBody HotelAdminRequest req, org.springframework.security.core.Authentication authentication) {
        String name = req.getName() != null ? req.getName().trim() : "";
        if (name.isBlank()) throw new RuntimeException("Vui lòng nhập tên chi nhánh.");
        if (hotelRepository.existsByNameIgnoreCase(name)) throw new RuntimeException("Tên chi nhánh đã tồn tại.");

        Hotel hotel = Hotel.builder()
                .name(name)
                .address(req.getAddress())
                .description(null)
                .phone(req.getPhone())
                .status("ACTIVE")
                .build();
        Hotel saved = hotelRepository.save(hotel);

        // Optional: assign manager to this branch.
        if (req.getManagerId() != null) {
            User mgr = userRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản Manager."));
            if (mgr.getRole() != UserRole.BRANCH_MANAGER) throw new RuntimeException("Tài khoản được chọn không phải Manager.");
            if (mgr.getBranch() != null) throw new RuntimeException("Manager đang thuộc một chi nhánh khác.");
            mgr.setBranch(saved);
            userRepository.save(mgr);
        }

        log.info("[ADMIN] create hotel: hotelId={}, name={}", saved.getId(), saved.getName());
        activityLogService.log(authentication, "ADMIN_BRANCH_CREATE", "BRANCH", String.valueOf(saved.getId()),
                "name=" + saved.getName() + ",status=" + saved.getStatus());
        return HotelAdminResponse.fromEntity(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public HotelAdminResponse update(@PathVariable Integer id, @Valid @RequestBody HotelAdminRequest req, org.springframework.security.core.Authentication authentication) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
        String name = req.getName() != null ? req.getName().trim() : "";
        if (name.isBlank()) throw new RuntimeException("Vui lòng nhập tên chi nhánh.");
        if (!name.equalsIgnoreCase(hotel.getName()) && hotelRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("Tên chi nhánh đã tồn tại.");
        }
        hotel.setName(name);
        hotel.setAddress(req.getAddress());
        hotel.setPhone(req.getPhone());
        if (req.getStatus() != null && !req.getStatus().isBlank()) hotel.setStatus(req.getStatus().trim().toUpperCase());

        Hotel saved = hotelRepository.save(hotel);

        // Optional: assign manager
        if (req.getManagerId() != null) {
            User mgr = userRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản Manager."));
            if (mgr.getRole() != UserRole.BRANCH_MANAGER) throw new RuntimeException("Tài khoản được chọn không phải Manager.");
            if (mgr.getBranch() != null && !mgr.getBranch().getId().equals(saved.getId())) {
                throw new RuntimeException("Manager đang thuộc một chi nhánh khác.");
            }
            mgr.setBranch(saved);
            userRepository.save(mgr);
        }

        log.info("[ADMIN] update hotel: hotelId={}", saved.getId());
        activityLogService.log(authentication, "ADMIN_BRANCH_UPDATE", "BRANCH", String.valueOf(saved.getId()),
                "name=" + saved.getName() + ",status=" + saved.getStatus());
        return HotelAdminResponse.fromEntity(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Integer id, org.springframework.security.core.Authentication authentication) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));
        if ("ACTIVE".equalsIgnoreCase(hotel.getStatus())) {
            throw new RuntimeException("Không thể xoá chi nhánh đang hoạt động.");
        }

        log.info("[ADMIN] delete hotel start: hotelId={}, status={}", id, hotel.getStatus());

        // Revoke all users assigned to this branch (manager/receptionist) permanently.
        List<User> users = userRepository.findByBranch_Id(id);
        if (!users.isEmpty()) {
            SecureRandom rnd = new SecureRandom();
            for (User u : users) {
                byte[] buf = new byte[32];
                rnd.nextBytes(buf);
                String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
                u.setPassword(passwordEncoder.encode(secret));
                u.setRole(UserRole.SYSTEM);
                u.setBranch(null);
            }
            userRepository.saveAll(users);
        }

        // Delete bookings/invoices linked to rooms of this branch.
        List<Integer> bookingIds = bookingRoomRepository.findBookingIdsByHotelId(id);
        if (!bookingIds.isEmpty()) {
            invoiceRepository.deleteByBookingIds(bookingIds);
            bookingRoomRepository.deleteByBookingIds(bookingIds);
            bookingRepository.deleteByIds(bookingIds);
        }

        // Delete rooms of this branch and finally the branch itself.
        roomRepository.deleteByHotel_Id(id);
        hotelRepository.delete(hotel);

        log.info("[ADMIN] delete hotel done: hotelId={}", id);
        activityLogService.log(authentication, "ADMIN_BRANCH_DELETE", "BRANCH", String.valueOf(id), "deleted");
    }
}

