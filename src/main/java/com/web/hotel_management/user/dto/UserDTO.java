package com.web.hotel_management.user.dto;

import com.web.hotel_management.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO khớp bảng {@code tblUser}: ID, username, fullName, position, mail, description.
 * Không trả về password.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Integer id;
    private String username;
    private String fullName;
    private String position;
    private String mail;
    private String description;

    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .position(user.getPosition())
                .mail(user.getMail())
                .description(user.getDescription())
                .build();
    }
}
