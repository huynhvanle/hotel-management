package com.web.hotel_management.branch.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BranchReceptionistItemResponse {
    private Integer id;
    private String username;
    private String fullName;
    private String phone;
    private Integer branchId;
    private String branchName;
}

