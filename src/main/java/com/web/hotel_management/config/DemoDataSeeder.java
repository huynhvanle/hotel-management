package com.web.hotel_management.config;

import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.repository.RoomRepository;
import com.web.hotel_management.user.entity.User;
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
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!userRepository.existsByUsername("system")) {
                userRepository.save(User.builder()
                        .username("system")
                        .password(passwordEncoder.encode("system"))
                        .fullName("System User")
                        .position("USER")
                        .mail("system@local")
                        .description("Seed user for public bookings")
                        .build());
            }

            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .fullName("Admin User")
                        .position("ADMIN")
                        .mail("admin@local")
                        .description("Seed admin account")
                        .build());
            }

            if (hotelRepository.count() > 0 || roomRepository.count() > 0) return;

            Hotel hotel = hotelRepository.save(Hotel.builder()
                    .name("Hanoi Hotel")
                    .starLevel(4)
                    .address("Hà Nội, Việt Nam")
                    .description("")
                    .build());

            List<Room> rooms = List.of(
                    Room.builder()
                            .id("R-101")
                            .name("Standard City View")
                            .type("Standard")
                            .price(850_000d)
                            .description("Phòng tiêu chuẩn, phù hợp cho 1-2 người.")
                            .hotel(hotel)
                            .build(),
                    Room.builder()
                            .id("R-201")
                            .name("Deluxe Balcony")
                            .type("Deluxe")
                            .price(1_250_000d)
                            .description("Phòng deluxe rộng rãi, có ban công.")
                            .hotel(hotel)
                            .build(),
                    Room.builder()
                            .id("R-301")
                            .name("Suite Premium")
                            .type("Suite")
                            .price(2_150_000d)
                            .description("Suite cao cấp, trải nghiệm tiện nghi đầy đủ.")
                            .hotel(hotel)
                            .build()
            );

            roomRepository.saveAll(rooms);
        };
    }
}
