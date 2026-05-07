package com.web.hotel_management.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private String bankName;
    private String accountNumber;
    private String accountName;
    private String transferContentTemplate;
    private String qrImageUrl;
}

