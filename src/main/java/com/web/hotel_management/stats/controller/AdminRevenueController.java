package com.web.hotel_management.stats.controller;

import com.web.hotel_management.stats.dto.RevenueStatsResponse;
import com.web.hotel_management.stats.service.AdminRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRevenueController {

    private final AdminRevenueService adminRevenueService;

    @GetMapping("/revenue")
    public RevenueStatsResponse revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminRevenueService.revenue(from, to);
    }
}
