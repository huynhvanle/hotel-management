package com.web.hotel_management.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInfoResponse {
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContentTemplate;
    private String qrImageUrl;
    private Double suggestedDepositPercent;
}

