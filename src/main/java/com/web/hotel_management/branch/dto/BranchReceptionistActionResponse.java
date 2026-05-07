package com.web.hotel_management.branch.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchReceptionistActionResponse {
    private Integer id;
    private String message;
}

