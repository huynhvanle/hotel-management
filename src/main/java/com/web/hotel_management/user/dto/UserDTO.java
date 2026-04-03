package com.web.hotel_management.user.dto;

import com.web.hotel_management.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Integer id;
    private String username;
    private String fullName;
    private String idCardNumber;
    private String address;
    private String email;
    private String phone;
    private String role;
    private String description;

    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .idCardNumber(user.getIdCardNumber())
                .address(user.getAddress())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .description(user.getDescription())
                .build();
    }
}
