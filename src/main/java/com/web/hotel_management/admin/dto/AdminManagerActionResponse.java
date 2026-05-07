package com.web.hotel_management.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminManagerActionResponse {
    private Integer id;
    private String message;
}

