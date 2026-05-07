package com.web.hotel_management.reception.controller;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingRoom;
import com.web.hotel_management.booking.entity.BookingStatus;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.booking.service.BookingPricingHelper;
import com.web.hotel_management.reception.dto.ReceptionCheckinOptionsResponse;
import com.web.hotel_management.reception.dto.ReceptionCheckinRequest;
import com.web.hotel_management.reception.dto.ReceptionBookingDetailResponse;
import com.web.hotel_management.reception.dto.ReceptionBookingConfirmResponse;
import com.web.hotel_management.reception.dto.ReceptionBookingSearchItem;
import com.web.hotel_management.reception.dto.ReceptionInvoicePreviewResponse;
import com.web.hotel_management.reception.dto.ReceptionInvoicePayResponse;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.invoice.entity.Invoice;
import com.web.hotel_management.invoice.repository.InvoiceRepository;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reception/bookings")
@RequiredArgsConstructor
public class ReceptionBookingController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final BookingPricingHelper bookingPricingHelper;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
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

    @GetMapping("/search")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public List<ReceptionBookingSearchItem> search(
            @RequestParam(name = "bookingId", required = false) Integer bookingId,
            @RequestParam(name = "phone", required = false) String phone,
            Authentication authentication
    ) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] search bookings: user={}, bookingId={}, phone={}", u, bookingId, phone);
        activityLogService.log(authentication, "BOOKING_SEARCH", "BOOKING", null,
                "bookingId=" + bookingId + ",phone=" + phone);
        return bookingRepository.searchByIdAndPhone(bookingId, phone).stream()
                .map(b -> {
                    List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
                    double total = bookingPricingHelper.stayTotalFromBookingRooms(brs, b.getCheckin(), b.getCheckout());
                    double deposit = bookingPricingHelper.depositFromTotal(total);
                    return ReceptionBookingSearchItem.builder()
                            .bookingId(b.getId())
                            .clientPhone(b.getClient() != null ? b.getClient().getPhone() : null)
                            .checkin(b.getCheckin())
                            .checkout(b.getCheckout())
                            .checkedIn(b.getCheckedInAt() != null)
                            .checkedOut(b.getCheckedOutAt() != null)
                            .status(b.getStatus())
                            .depositAmount(deposit)
                            .build();
                })
                .toList();
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionBookingDetailResponse detail(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] view booking detail: user={}, bookingId={}", u, bookingId);
        activityLogService.log(authentication, "BOOKING_DETAIL_VIEW", "BOOKING", String.valueOf(bookingId), null);
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        boolean checkedIn = b.getCheckedInAt() != null;
        boolean invoiceSaved = invoiceRepository.existsByBooking_Id(b.getId());
        boolean checkedOut = b.getCheckedOutAt() != null;

        // Aggregate room types + quantities for display before check-in.
        Map<Integer, ReceptionBookingDetailResponse.RoomTypeLine.RoomTypeLineBuilder> roomTypeBuilders = new LinkedHashMap<>();
        Map<Integer, Integer> roomTypeQty = new LinkedHashMap<>();
        for (BookingRoom br : brs) {
            Integer rtId = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getId() : null;
            String rtName = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getName() : null;
            if (rtId == null) continue;
            roomTypeQty.put(rtId, roomTypeQty.getOrDefault(rtId, 0) + 1);
            if (!roomTypeBuilders.containsKey(rtId)) {
                roomTypeBuilders.put(rtId, ReceptionBookingDetailResponse.RoomTypeLine.builder()
                        .roomTypeId(rtId)
                        .roomTypeName(rtName));
            }
        }
        List<ReceptionBookingDetailResponse.RoomTypeLine> roomTypes = roomTypeQty.entrySet().stream()
                .map(e -> roomTypeBuilders.get(e.getKey()).quantity(e.getValue()).build())
                .toList();

        List<String> roomIds = checkedIn
                ? brs.stream()
                    .map(br -> br.getRoom() != null ? br.getRoom().getId() : null)
                    .filter(x -> x != null && !x.isBlank())
                    .toList()
                : List.of();
        double total = bookingPricingHelper.stayTotalFromBookingRooms(brs, b.getCheckin(), b.getCheckout());
        double deposit = bookingPricingHelper.depositFromTotal(total);
        return ReceptionBookingDetailResponse.builder()
                .bookingId(b.getId())
                .clientPhone(b.getClient() != null ? b.getClient().getPhone() : null)
                .clientFullName(b.getClient() != null ? b.getClient().getFullName() : null)
                .checkin(b.getCheckin())
                .checkout(b.getCheckout())
                .status(b.getStatus())
                .depositAmount(deposit)
                .checkedIn(checkedIn)
                .invoiceSaved(invoiceSaved)
                .checkedOut(checkedOut)
                .roomTypes(roomTypes)
                .roomIds(roomIds)
                .build();
    }

    @PutMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionBookingConfirmResponse confirm(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] confirm booking: user={}, bookingId={}", u, bookingId);
        activityLogService.log(authentication, "BOOKING_CONFIRM", "BOOKING", String.valueOf(bookingId), null);
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));

        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Đơn đã hủy, không thể xác nhận.");
        }
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            return ReceptionBookingConfirmResponse.builder()
                    .bookingId(b.getId())
                    .status(b.getStatus())
                    .message("Đơn đã được xác nhận trước đó.")
                    .build();
        }
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Không thể xác nhận đơn ở trạng thái hiện tại.");
        }

        b.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(b);
        return ReceptionBookingConfirmResponse.builder()
                .bookingId(b.getId())
                .status(BookingStatus.CONFIRMED)
                .message("Xác nhận đơn đặt thành công.")
                .build();
    }

    @PutMapping("/{bookingId}/unconfirm")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionBookingConfirmResponse unconfirm(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] unconfirm booking: user={}, bookingId={}", u, bookingId);
        activityLogService.log(authentication, "BOOKING_UNCONFIRM", "BOOKING", String.valueOf(bookingId), null);
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));

        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Đơn đã hủy, không thể huỷ xác nhận.");
        }
        if (b.getCheckedInAt() != null) {
            throw new RuntimeException("Đơn đã check-in, không thể huỷ xác nhận.");
        }
        if (b.getStatus() == BookingStatus.PENDING) {
            return ReceptionBookingConfirmResponse.builder()
                    .bookingId(b.getId())
                    .status(b.getStatus())
                    .message("Đơn đang ở trạng thái chờ xác nhận.")
                    .build();
        }
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Không thể huỷ xác nhận đơn ở trạng thái hiện tại.");
        }

        b.setStatus(BookingStatus.PENDING);
        bookingRepository.save(b);
        return ReceptionBookingConfirmResponse.builder()
                .bookingId(b.getId())
                .status(BookingStatus.PENDING)
                .message("Đã huỷ xác nhận. Đơn trở về trạng thái chờ xác nhận.")
                .build();
    }

    @GetMapping("/{bookingId}/checkin-options")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionCheckinOptionsResponse checkinOptions(@PathVariable Integer bookingId, Authentication authentication) {
        Integer hotelId = resolveHotelId(authentication);
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] checkin options: user={}, bookingId={}, hotelId={}", u, bookingId, hotelId);
        activityLogService.log(authentication, "BOOKING_CHECKIN_OPTIONS_VIEW", "BOOKING", String.valueOf(bookingId),
                "hotelId=" + hotelId);

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ đơn đã xác nhận mới được check-in.");
        }
        if (b.getCheckedInAt() != null) {
            throw new RuntimeException("Đơn này đã check-in.");
        }
        LocalDate today = LocalDate.now();
        if (b.getCheckin() == null || b.getCheckout() == null) throw new RuntimeException("Thiếu ngày đặt.");
        if (today.isBefore(b.getCheckin()) || !today.isBefore(b.getCheckout())) {
            throw new RuntimeException("Chỉ cho phép check-in trong khoảng thời gian lưu trú.");
        }

        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        if (brs.isEmpty()) throw new RuntimeException("Đơn đặt chưa có phòng.");

        // Count required quantities per roomType (based on booking's rooms).
        Map<Integer, ReceptionCheckinOptionsResponse.RoomTypeOption.RoomTypeOptionBuilder> builders = new LinkedHashMap<>();
        Map<Integer, Integer> qty = new LinkedHashMap<>();
        for (BookingRoom br : brs) {
            Integer rtId = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getId() : null;
            String rtName = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getName() : null;
            if (rtId == null) continue;
            qty.put(rtId, qty.getOrDefault(rtId, 0) + 1);
            if (!builders.containsKey(rtId)) {
                builders.put(rtId, ReceptionCheckinOptionsResponse.RoomTypeOption.builder()
                        .roomTypeId(rtId)
                        .roomTypeName(rtName));
            }
        }
        if (qty.isEmpty()) throw new RuntimeException("Không xác định được loại phòng của đơn đặt.");

        List<ReceptionCheckinOptionsResponse.RoomTypeOption> roomTypes = qty.entrySet().stream()
                .map(e -> {
                    Integer rtId = e.getKey();
                    Integer q = e.getValue();
                    List<String> available = roomRepository.searchAvailableForCheckin(
                                    hotelId,
                                    rtId,
                                    b.getCheckin(),
                                    b.getCheckout(),
                                    b.getId()
                            ).stream()
                            .map(Room::getId)
                            .toList();
                    return builders.get(rtId)
                            .quantity(q)
                            .availableRoomIds(available)
                            .build();
                })
                .toList();

        return ReceptionCheckinOptionsResponse.builder()
                .bookingId(b.getId())
                .roomTypes(roomTypes)
                .build();
    }

    @PutMapping("/{bookingId}/checkin")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionBookingConfirmResponse checkin(
            @PathVariable Integer bookingId,
            @Valid @RequestBody ReceptionCheckinRequest req,
            Authentication authentication
    ) {
        Integer hotelId = resolveHotelId(authentication);
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] checkin: user={}, bookingId={}, hotelId={}, roomIds={}", u, bookingId, hotelId, req.getRoomIds());
        activityLogService.log(authentication, "BOOKING_CHECKIN", "BOOKING", String.valueOf(bookingId),
                "hotelId=" + hotelId + ",roomIds=" + String.valueOf(req.getRoomIds()));

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        if (b.getStatus() != BookingStatus.CONFIRMED) throw new RuntimeException("Chỉ đơn đã xác nhận mới được check-in.");
        if (b.getCheckedInAt() != null) throw new RuntimeException("Đơn này đã check-in.");

        LocalDate today = LocalDate.now();
        if (b.getCheckin() == null || b.getCheckout() == null) throw new RuntimeException("Thiếu ngày đặt.");
        if (today.isBefore(b.getCheckin()) || !today.isBefore(b.getCheckout())) {
            throw new RuntimeException("Chỉ cho phép check-in trong khoảng thời gian lưu trú.");
        }

        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        if (brs.isEmpty()) throw new RuntimeException("Đơn đặt chưa có phòng.");

        // Required counts by roomType
        Map<Integer, Integer> required = new LinkedHashMap<>();
        for (BookingRoom br : brs) {
            Integer rtId = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getId() : null;
            if (rtId == null) continue;
            required.put(rtId, required.getOrDefault(rtId, 0) + 1);
        }
        if (required.isEmpty()) throw new RuntimeException("Không xác định được loại phòng của đơn đặt.");

        List<String> picked = new LinkedHashSet<>(req.getRoomIds().stream().map(String::trim).filter(s -> !s.isBlank()).toList())
                .stream().toList();
        if (picked.isEmpty()) throw new RuntimeException("Vui lòng chọn phòng để check-in.");
        if (picked.size() != brs.size()) {
            throw new RuntimeException("Số phòng check-in phải đúng bằng số phòng đã đặt.");
        }

        List<Room> rooms = roomRepository.findAllById(picked);
        if (rooms.size() != picked.size()) throw new RuntimeException("Một hoặc nhiều phòng không tồn tại.");

        // Validate rooms are AVAILABLE, belong to branch, and match required roomType multiset.
        Map<Integer, Integer> actual = new LinkedHashMap<>();
        for (Room r : rooms) {
            if (r.getHotel() == null || r.getHotel().getId() == null || !r.getHotel().getId().equals(hotelId)) {
                throw new RuntimeException("Phòng không thuộc chi nhánh của bạn: " + r.getId());
            }
            if (r.getStatus() != RoomStatus.AVAILABLE) {
                throw new RuntimeException("Chỉ phòng trạng thái Có sẵn mới được check-in: " + r.getId());
            }
            Integer rtId = r.getRoomType() != null ? r.getRoomType().getId() : null;
            if (rtId == null) throw new RuntimeException("Phòng thiếu loại phòng: " + r.getId());
            actual.put(rtId, actual.getOrDefault(rtId, 0) + 1);
        }
        if (!actual.equals(required)) {
            throw new RuntimeException("Phòng được chọn không khớp loại phòng/số lượng đã đặt.");
        }

        // Ensure picked rooms are not overlapping with other active bookings (exclude current booking).
        for (Room r : rooms) {
            boolean ok = roomRepository.searchAvailableForCheckin(hotelId, r.getRoomType().getId(), b.getCheckin(), b.getCheckout(), b.getId())
                    .stream().anyMatch(x -> x.getId().equals(r.getId()));
            if (!ok) throw new RuntimeException("Phòng không còn trống theo lịch: " + r.getId());
        }

        // Replace booking rooms with picked rooms.
        bookingRoomRepository.deleteAll(brs);
        for (Room r : rooms) {
            bookingRoomRepository.save(BookingRoom.builder().booking(b).room(r).build());
            r.setStatus(RoomStatus.UNAVAILABLE);
        }
        roomRepository.saveAll(rooms);
        b.setCheckedInAt(LocalDateTime.now());
        bookingRepository.save(b);

        return ReceptionBookingConfirmResponse.builder()
                .bookingId(b.getId())
                .status(b.getStatus())
                .message("Check-in thành công.")
                .build();
    }

    @GetMapping("/{bookingId}/invoice-preview")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionInvoicePreviewResponse invoicePreview(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] invoice preview: user={}, bookingId={}", u, bookingId);
        activityLogService.log(authentication, "INVOICE_PREVIEW", "BOOKING", String.valueOf(bookingId), null);

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        if (b.getCheckedInAt() == null) {
            throw new RuntimeException("Đơn này chưa check-in.");
        }
        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        if (brs.isEmpty()) throw new RuntimeException("Đơn đặt chưa có phòng.");

        Map<Integer, ReceptionInvoicePreviewResponse.RoomTypeLine.RoomTypeLineBuilder> roomTypeBuilders = new LinkedHashMap<>();
        Map<Integer, Integer> roomTypeQty = new LinkedHashMap<>();
        for (BookingRoom br : brs) {
            Integer rtId = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getId() : null;
            String rtName = br.getRoom() != null && br.getRoom().getRoomType() != null ? br.getRoom().getRoomType().getName() : null;
            if (rtId == null) continue;
            roomTypeQty.put(rtId, roomTypeQty.getOrDefault(rtId, 0) + 1);
            if (!roomTypeBuilders.containsKey(rtId)) {
                roomTypeBuilders.put(rtId, ReceptionInvoicePreviewResponse.RoomTypeLine.builder()
                        .roomTypeId(rtId)
                        .roomTypeName(rtName));
            }
        }
        List<ReceptionInvoicePreviewResponse.RoomTypeLine> roomTypes = roomTypeQty.entrySet().stream()
                .map(e -> roomTypeBuilders.get(e.getKey()).quantity(e.getValue()).build())
                .toList();

        List<String> roomIds = brs.stream()
                .map(br -> br.getRoom() != null ? br.getRoom().getId() : null)
                .filter(x -> x != null && !x.isBlank())
                .toList();

        double total = bookingPricingHelper.stayTotalFromBookingRooms(brs, b.getCheckin(), b.getCheckout());
        double deposit = bookingPricingHelper.depositFromTotal(total);
        double remaining = Math.max(0d, total - deposit);

        return ReceptionInvoicePreviewResponse.builder()
                .bookingId(b.getId())
                .checkedIn(true)
                .clientFullName(b.getClient() != null ? b.getClient().getFullName() : null)
                .clientPhone(b.getClient() != null ? b.getClient().getPhone() : null)
                .checkin(b.getCheckin())
                .checkout(b.getCheckout())
                .totalAmount(total)
                .depositAmount(deposit)
                .remainingAmount(remaining)
                .roomTypes(roomTypes)
                .roomIds(roomIds)
                .build();
    }

    @PutMapping("/{bookingId}/invoice/pay")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionInvoicePayResponse payInvoice(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] invoice pay: user={}, bookingId={}", u, bookingId);

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        if (b.getCheckedInAt() == null) throw new RuntimeException("Đơn này chưa check-in.");

        if (invoiceRepository.existsByBooking_Id(b.getId())) {
            Invoice ex = invoiceRepository.findByBooking_Id(b.getId()).orElseThrow();
            activityLogService.log(authentication, "INVOICE_PAY_DUPLICATE", "INVOICE", String.valueOf(ex.getId()),
                    "bookingId=" + b.getId());
            return ReceptionInvoicePayResponse.builder()
                    .bookingId(b.getId())
                    .invoiceId(ex.getId())
                    .paidAmount(ex.getPaidAmount())
                    .paidAt(ex.getPaidAt())
                    .message("Đơn đã được ghi nhận thanh toán trước đó.")
                    .build();
        }

        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        if (brs.isEmpty()) throw new RuntimeException("Đơn đặt chưa có phòng.");

        double total = bookingPricingHelper.stayTotalFromBookingRooms(brs, b.getCheckin(), b.getCheckout());
        double deposit = bookingPricingHelper.depositFromTotal(total);
        LocalDateTime now = LocalDateTime.now();

        Invoice inv = invoiceRepository.save(Invoice.builder()
                .booking(b)
                .totalAmount(total)
                .depositAmount(deposit)
                .paidAmount(total) // đã thanh toán đủ
                .issuedAt(now)
                .paidAt(now)
                .build());
        activityLogService.log(authentication, "INVOICE_PAY", "INVOICE", String.valueOf(inv.getId()),
                "bookingId=" + b.getId() + ",paidAmount=" + inv.getPaidAmount());

        return ReceptionInvoicePayResponse.builder()
                .bookingId(b.getId())
                .invoiceId(inv.getId())
                .paidAmount(inv.getPaidAmount())
                .paidAt(inv.getPaidAt())
                .message("Đã ghi nhận khách thanh toán đủ. Hoá đơn đã được lưu.")
                .build();
    }

    @PutMapping("/{bookingId}/checkout")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionBookingConfirmResponse checkout(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] checkout: user={}, bookingId={}", u, bookingId);
        activityLogService.log(authentication, "BOOKING_CHECKOUT", "BOOKING", String.valueOf(bookingId), null);

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        if (b.getCheckedInAt() == null) throw new RuntimeException("Đơn này chưa check-in.");
        if (b.getCheckedOutAt() != null) {
            return ReceptionBookingConfirmResponse.builder()
                    .bookingId(b.getId())
                    .status(b.getStatus())
                    .message("Đơn đã check-out trước đó.")
                    .build();
        }
        if (!invoiceRepository.existsByBooking_Id(b.getId())) {
            throw new RuntimeException("Chưa lưu hoá đơn/ thanh toán. Không thể check-out.");
        }

        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        List<String> roomIds = brs.stream()
                .map(br -> br.getRoom() != null ? br.getRoom().getId() : null)
                .filter(x -> x != null && !x.isBlank())
                .toList();
        if (roomIds.isEmpty()) throw new RuntimeException("Không có phòng để check-out.");

        List<Room> rooms = roomRepository.findAllById(roomIds);
        for (Room r : rooms) {
            // trả phòng về Có sẵn (đúng kho phòng trống)
            r.setStatus(RoomStatus.AVAILABLE);
        }
        roomRepository.saveAll(rooms);

        b.setCheckedOutAt(LocalDateTime.now());
        bookingRepository.save(b);

        return ReceptionBookingConfirmResponse.builder()
                .bookingId(b.getId())
                .status(b.getStatus())
                .message("Check-out thành công.")
                .build();
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public ReceptionBookingConfirmResponse cancel(@PathVariable Integer bookingId, Authentication authentication) {
        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[RECEPTION] cancel booking: user={}, bookingId={}", u, bookingId);
        activityLogService.log(authentication, "BOOKING_CANCEL", "BOOKING", String.valueOf(bookingId), null);

        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt."));
        if (b.getStatus() == BookingStatus.CANCELLED) {
            return ReceptionBookingConfirmResponse.builder()
                    .bookingId(b.getId())
                    .status(b.getStatus())
                    .message("Đơn đã hủy trước đó.")
                    .build();
        }
        if (b.getCheckedInAt() != null) {
            throw new RuntimeException("Đơn đã check-in, không thể hủy. Vui lòng check-out.");
        }
        if (b.getCheckedOutAt() != null) {
            throw new RuntimeException("Đơn đã check-out, không thể hủy.");
        }

        // Nếu có phòng vật lí đã được gán trước đó (trường hợp dữ liệu cũ), trả về Có sẵn.
        List<BookingRoom> brs = bookingRoomRepository.findByBooking_Id(b.getId());
        List<String> roomIds = brs.stream()
                .map(br -> br.getRoom() != null ? br.getRoom().getId() : null)
                .filter(x -> x != null && !x.isBlank())
                .toList();
        if (!roomIds.isEmpty()) {
            List<Room> rooms = roomRepository.findAllById(roomIds);
            for (Room r : rooms) r.setStatus(RoomStatus.AVAILABLE);
            roomRepository.saveAll(rooms);
        }

        b.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(b);

        return ReceptionBookingConfirmResponse.builder()
                .bookingId(b.getId())
                .status(BookingStatus.CANCELLED)
                .message("Hủy đơn thành công.")
                .build();
    }
}

