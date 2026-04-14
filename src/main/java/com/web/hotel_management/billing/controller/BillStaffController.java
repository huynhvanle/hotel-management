package com.web.hotel_management.billing.controller;

import com.web.hotel_management.billing.dto.BillStaffRequest;
import com.web.hotel_management.billing.dto.BillStaffResponse;
import com.web.hotel_management.billing.entity.Bill;
import com.web.hotel_management.billing.repository.BillRepository;
import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookedRoom;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.booking.repository.BookedRoomRepository;
import com.web.hotel_management.booking.repository.UsedServiceRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/staff/bills")
public class BillStaffController {

    private final BillRepository billRepository;
    private final BookingRepository bookingRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final UsedServiceRepository usedServiceRepository;
    private final UserRepository userRepository;

    public BillStaffController(
            BillRepository billRepository,
            BookingRepository bookingRepository,
            BookedRoomRepository bookedRoomRepository,
            UsedServiceRepository usedServiceRepository,
            UserRepository userRepository
    ) {
        this.billRepository = billRepository;
        this.bookingRepository = bookingRepository;
        this.bookedRoomRepository = bookedRoomRepository;
        this.usedServiceRepository = usedServiceRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<BillStaffResponse> list() {
        return billRepository.findAll().stream().map(BillStaffResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public BillStaffResponse get(@PathVariable Integer id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with id: " + id));
        return BillStaffResponse.fromEntity(bill);
    }

    @GetMapping("/by-booking")
    public BillStaffResponse getByBooking(@RequestParam Integer bookingId) {
        Bill bill = billRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Bill not found for booking: " + bookingId));
        return BillStaffResponse.fromEntity(bill);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BillStaffResponse create(@Valid @RequestBody BillStaffRequest req, Authentication authentication) {
        if (billRepository.findByBooking_Id(req.getBookingId()).isPresent()) {
            throw new RuntimeException("Bill already exists for booking: " + req.getBookingId());
        }
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + req.getBookingId()));

        String username = authentication.getName();
        User receptionist = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Double amount = calculateAmount(req.getBookingId());

        Bill bill = Bill.builder()
                .paymentDate(req.getPaymentDate())
                .paymentAmount(amount)
                .paymentType(req.getPaymentType())
                .note(req.getNote())
                .booking(booking)
                .receptionist(receptionist)
                .build();
        return BillStaffResponse.fromEntity(billRepository.save(bill));
    }

    @PutMapping("/{id}")
    public BillStaffResponse update(@PathVariable Integer id, @Valid @RequestBody BillStaffRequest req, Authentication authentication) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found with id: " + id));

        // do not allow switching booking once created
        if (!bill.getBooking().getId().equals(req.getBookingId())) {
            throw new RuntimeException("Cannot change bookingId of an existing bill");
        }

        String username = authentication.getName();
        User receptionist = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Double amount = calculateAmount(req.getBookingId());

        bill.setPaymentDate(req.getPaymentDate());
        bill.setPaymentAmount(amount);
        bill.setPaymentType(req.getPaymentType());
        bill.setNote(req.getNote());
        bill.setReceptionist(receptionist);
        return BillStaffResponse.fromEntity(billRepository.save(bill));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!billRepository.existsById(id)) {
            throw new RuntimeException("Bill not found with id: " + id);
        }
        billRepository.deleteById(id);
    }

    @GetMapping("/calc")
    public Double calculate(@RequestParam Integer bookingId) {
        return calculateAmount(bookingId);
    }

    private Double calculateAmount(Integer bookingId) {
        List<BookedRoom> rooms = bookedRoomRepository.findByBooking_Id(bookingId);
        if (rooms.isEmpty()) return 0.0;

        double total = 0.0;
        for (BookedRoom br : rooms) {
            double pricePerNight = br.getRoom() != null && br.getRoom().getPrice() != null ? br.getRoom().getPrice() : 0.0;
            long nights = br.getCheckin() != null && br.getCheckout() != null
                    ? ChronoUnit.DAYS.between(br.getCheckin(), br.getCheckout())
                    : 0;
            if (nights < 0) nights = 0;
            total += pricePerNight * nights;

            // add used services if any
            var used = usedServiceRepository.findByBookedRoom_Id(br.getId());
            for (var us : used) {
                if (us.getService() != null && us.getService().getPrice() != null && us.getQuantity() != null) {
                    total += us.getService().getPrice().doubleValue() * us.getQuantity();
                }
            }
        }
        return total;
    }
}

