package com.web.hotel_management.config;

import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.repository.RoomRepository;
import com.web.hotel_management.roomtype.entity.RoomType;
import com.web.hotel_management.roomtype.entity.RoomTypeStatus;
import com.web.hotel_management.roomtype.repository.RoomTypeRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import com.web.hotel_management.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@Profile("!test")
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoData(
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            // ===== Seed core staff accounts (login by username/password) =====
            if (!userRepository.existsByUsername("system")) {
                userRepository.save(User.builder()
                        .username("system")
                        .password(passwordEncoder.encode("system1234"))
                        .fullName("System User")
                        .role(UserRole.SYSTEM)
                        .phone("0000000000")
                        .build());
            }

            if (!userRepository.existsByUsername("admin123")) {
                userRepository.save(User.builder()
                        .username("admin123")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Admin User")
                        .role(UserRole.ADMIN)
                        .phone("0000000001")
                        .build());
            }

            // ===== Seed a default branch if empty =====
            Hotel hotel;
            if (hotelRepository.count() == 0) {
                hotel = hotelRepository.save(Hotel.builder()
                        .name("Hanoi Branch")
                        .address("Hà Nội, Việt Nam")
                        .description(
                                "Chi nhánh trung tâm Vie Hotel tại Hà Nội — view phố cổ, tiện di chuyển ra sân bay và khu vực hội nghị."
                                        + " Không gian hiện đại, ấm cúng; lễ tân hỗ trợ 24/7.")
                        .phone("0123456789")
                        .status("ACTIVE")
                        .build());
            } else {
                hotel = hotelRepository.findAll().stream().findFirst().orElseThrow();
            }

            // ===== Seed branch staff tied to the branch =====
            if (!userRepository.existsByUsername("manager1")) {
                userRepository.save(User.builder()
                        .username("manager1")
                        .password(passwordEncoder.encode("manager112"))
                        .fullName("Branch Manager")
                        .role(UserRole.BRANCH_MANAGER)
                        .phone("0000000002")
                        .branch(hotel)
                        .build());
            }

            if (!userRepository.existsByUsername("receptionist1")) {
                userRepository.save(User.builder()
                        .username("receptionist1")
                        .password(passwordEncoder.encode("receptionist1"))
                        .fullName("Receptionist")
                        .role(UserRole.RECEPTIONIST)
                        .phone("0000000003")
                        .branch(hotel)
                        .build());
            }

            // If we already have data, don't seed demo rooms/types again.
            if (roomRepository.count() > 0) return;

            RoomType standard = roomTypeRepository.save(RoomType.builder()
                    .code("STANDARD")
                    .name("Standard")
                    .description("Phòng tiêu chuẩn")
                    .basePrice(850_000d)
                    .status(RoomTypeStatus.ACTIVE)
                    .build());
            RoomType deluxe = roomTypeRepository.save(RoomType.builder()
                    .code("DELUXE")
                    .name("Deluxe")
                    .description("Phòng deluxe")
                    .basePrice(1_250_000d)
                    .status(RoomTypeStatus.ACTIVE)
                    .build());
            RoomType suite = roomTypeRepository.save(RoomType.builder()
                    .code("SUITE")
                    .name("Suite")
                    .description("Phòng suite")
                    .basePrice(2_150_000d)
                    .status(RoomTypeStatus.ACTIVE)
                    .build());

            List<Room> rooms = List.of(
                    Room.builder()
                            .id("101")
                            .floor(1)
                            .status(com.web.hotel_management.room.entity.RoomStatus.AVAILABLE)
                            .roomType(standard)
                            .hotel(hotel)
                            .build(),
                    Room.builder()
                            .id("201")
                            .floor(2)
                            .status(com.web.hotel_management.room.entity.RoomStatus.AVAILABLE)
                            .roomType(deluxe)
                            .hotel(hotel)
                            .build(),
                    Room.builder()
                            .id("301")
                            .floor(3)
                            .status(com.web.hotel_management.room.entity.RoomStatus.AVAILABLE)
                            .roomType(suite)
                            .hotel(hotel)
                            .build()
            );

            roomRepository.saveAll(rooms);
        };
    }
}
