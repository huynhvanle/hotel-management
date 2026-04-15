package com.web.hotel_management.stats.service;

import com.web.hotel_management.booking.entity.BookedRoom;
import com.web.hotel_management.booking.repository.BookedRoomRepository;
import com.web.hotel_management.stats.dto.RevenueLineDto;
import com.web.hotel_management.stats.dto.RevenueStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRevenueService {

    private final BookedRoomRepository bookedRoomRepository;

    /**
     * Estimated revenue by <b>booking date</b> (Booking.bookingDate) in [from, to].
     * Each BookedRoom line: nights × room price − line discount (if any).
     */
    public RevenueStatsResponse revenue(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (from == null) {
            from = today.withDayOfMonth(1);
        }
        if (to == null) {
            to = today;
        }
        if (from.isAfter(to)) {
            throw new RuntimeException("Start date must be on or before end date.");
        }

        List<BookedRoom> rows = bookedRoomRepository.findForRevenueByBookingDateBetween(from, to);
        Set<Integer> bookingIds = new HashSet<>();
        double total = 0;
        List<RevenueLineDto> lines = new ArrayList<>();

        for (BookedRoom br : rows) {
            if (br.getBooking() != null) {
                bookingIds.add(br.getBooking().getId());
            }
            long nights = nightsBetween(br.getCheckin(), br.getCheckout());
            double price = br.getRoom() != null && br.getRoom().getPrice() != null ? br.getRoom().getPrice() : 0;
            double discount = br.getDiscount() != null ? br.getDiscount() : 0;
            double amount = Math.max(0, nights * price - discount);

            total += amount;

            lines.add(RevenueLineDto.builder()
                    .bookingId(br.getBooking() != null ? br.getBooking().getId() : null)
                    .roomId(br.getRoom() != null ? br.getRoom().getId() : null)
                    .roomName(br.getRoom() != null ? br.getRoom().getName() : null)
                    .bookingDate(br.getBooking() != null ? br.getBooking().getBookingDate() : null)
                    .checkin(br.getCheckin())
                    .checkout(br.getCheckout())
                    .nights(nights)
                    .amount(round2(amount))
                    .build());
        }

        return RevenueStatsResponse.builder()
                .from(from)
                .to(to)
                .basis("By booking date (bookingDate); each line = nights × room price − line discount.")
                .distinctBookingCount(bookingIds.size())
                .bookedRoomLineCount(rows.size())
                .totalRevenue(round2(total))
                .lines(lines)
                .build();
    }

    private static long nightsBetween(LocalDate checkin, LocalDate checkout) {
        if (checkin == null || checkout == null || !checkout.isAfter(checkin)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(checkin, checkout);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
