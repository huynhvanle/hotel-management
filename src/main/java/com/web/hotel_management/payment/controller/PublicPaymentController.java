package com.web.hotel_management.payment.controller;

import com.web.hotel_management.payment.config.PaymentProperties;
import com.web.hotel_management.payment.dto.PaymentInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicPaymentController {

    private static final double DEPOSIT_PERCENT = 50.0;

    private final PaymentProperties paymentProperties;

    @GetMapping("/payment-info")
    public PaymentInfoResponse paymentInfo() {
        return PaymentInfoResponse.builder()
                .bankName(paymentProperties.getBankName())
                .accountNumber(paymentProperties.getAccountNumber())
                .accountName(paymentProperties.getAccountName())
                // Transfer content for deposit: use client phone (SĐT).
                .transferContentTemplate("{phone}")
                .qrImageUrl(paymentProperties.getQrImageUrl())
                .suggestedDepositPercent(DEPOSIT_PERCENT)
                .build();
    }
}

