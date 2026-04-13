package com.web.hotel_management.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateBookingRequest {
    /**
     * Backward-compatible: if provided, server will treat it as a single room booking.
     */
    private String roomId;

    /**
     * New: book multiple rooms in one booking.
     */
    private List<@NotBlank String> roomIds;

    @NotNull
    private LocalDate checkin;

    @NotNull
    private LocalDate checkout;

    private String note;

    @Valid
    @NotNull
    private ClientInput client;

    @Data
    public static class ClientInput {
        @NotNull
        @Positive
        private Long idCardNumber;

        @NotBlank
        private String fullName;

        @NotBlank
        private String email;

        private String phone;
        private String address;
        private String description;
    }
}
