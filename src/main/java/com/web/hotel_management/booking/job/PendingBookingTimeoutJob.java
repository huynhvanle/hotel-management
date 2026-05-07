package com.web.hotel_management.booking.job;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingStatus;
import com.web.hotel_management.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingBookingTimeoutJob {

    private final BookingRepository bookingRepository;

    /** Mỗi phút: huỷ các đơn PENDING quá 30 phút chưa xác nhận. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredPending() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Booking> expired = bookingRepository.findPendingCreatedBefore(BookingStatus.PENDING, cutoff);
        if (expired.isEmpty()) return;
        for (Booking b : expired) {
            b.setStatus(BookingStatus.CANCELLED);
        }
        bookingRepository.saveAll(expired);
        log.info("[BOOKING] auto-cancel pending expired: count={}, cutoff={}", expired.size(), cutoff);
    }
}

