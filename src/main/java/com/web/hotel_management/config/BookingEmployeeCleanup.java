package com.web.hotel_management.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * One-time schema/data cleanup to remove Booking -> User FK.
 * User requested to drop employee link in Booking.
 */
@Configuration
@Profile("!test")
public class BookingEmployeeCleanup {

    @Bean
    CommandLineRunner cleanupBookingEmployeeFk(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                // Find FK constraint name for Booking.userID -> User.id
                String sql = """
                        select CONSTRAINT_NAME
                        from information_schema.KEY_COLUMN_USAGE
                        where TABLE_SCHEMA = database()
                          and TABLE_NAME = 'Booking'
                          and COLUMN_NAME = 'userID'
                          and REFERENCED_TABLE_NAME is not null
                        """;
                List<String> fks = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString(1));
                for (String fk : fks) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE Booking DROP FOREIGN KEY " + fk);
                    } catch (Exception ignored) {
                        // ignore
                    }
                }

                // Make column nullable (if exists) and detach existing bookings from users
                jdbcTemplate.execute("ALTER TABLE Booking MODIFY userID INT NULL");
                jdbcTemplate.execute("UPDATE Booking SET userID = NULL");
            } catch (Exception ignored) {
                // If DB doesn't support information_schema query or column doesn't exist, ignore.
            }
        };
    }
}

