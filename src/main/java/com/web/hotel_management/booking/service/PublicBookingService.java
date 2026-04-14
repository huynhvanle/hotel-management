package com.web.hotel_management.booking.service;

import com.web.hotel_management.booking.dto.BookingResponse;
import com.web.hotel_management.booking.dto.CreateBookingRequest;
import com.web.hotel_management.booking.entity.BookedRoom;
import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.repository.BookedRoomRepository;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PublicBookingService {
    private final BookingRepository bookingRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;

    public PublicBookingService(
            BookingRepository bookingRepository,
            BookedRoomRepository bookedRoomRepository,
            ClientRepository clientRepository,
            RoomRepository roomRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookedRoomRepository = bookedRoomRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
    }

    public BookingResponse createPublicBooking(CreateBookingRequest req) {
        if (req.getCheckout().isBefore(req.getCheckin()) || req.getCheckout().isEqual(req.getCheckin())) {
            throw new RuntimeException("Checkout must be after checkin");
        }

        List<String> roomIds = resolveRoomIds(req);
        List<Room> rooms = roomRepository.findAllById(roomIds);
        if (rooms.size() != roomIds.size()) {
            Set<String> found = new LinkedHashSet<>(rooms.stream().map(Room::getId).toList());
            String missing = roomIds.stream().filter(id -> !found.contains(id)).findFirst().orElse("unknown");
            throw new RuntimeException("Room not found with id: " + missing);
        }

        // Prevent double-booking: overlap check for each room
        for (String roomId : roomIds) {
            if (bookedRoomRepository.existsOverlappingBooking(roomId, req.getCheckin(), req.getCheckout())) {
                throw new RuntimeException("Room is already booked for selected dates: " + roomId);
            }
        }

        Client client = upsertClient(req.getClient());

        Booking booking = Booking.builder()
                .bookingDate(LocalDate.now())
                .discount(0.0)
                .note(req.getNote())
                .client(client)
                .build();
        Booking savedBooking = bookingRepository.save(booking);

        Integer firstBookedRoomId = null;
        for (Room room : rooms) {
            BookedRoom bookedRoom = BookedRoom.builder()
                    .checkin(req.getCheckin())
                    .checkout(req.getCheckout())
                    .discount(0.0)
                    .isCheckedIn(0)
                    .note(req.getNote())
                    .booking(savedBooking)
                    .room(room)
                    .build();
            BookedRoom savedBookedRoom = bookedRoomRepository.save(bookedRoom);
            if (firstBookedRoomId == null) firstBookedRoomId = savedBookedRoom.getId();
        }

        return BookingResponse.builder()
                .bookingId(savedBooking.getId())
                .bookedRoomId(firstBookedRoomId)
                .build();
    }

    private static List<String> resolveRoomIds(CreateBookingRequest req) {
        if (req.getRoomIds() != null && !req.getRoomIds().isEmpty()) {
            // preserve order, remove duplicates
            return new LinkedHashSet<>(req.getRoomIds()).stream().toList();
        }
        if (req.getRoomId() != null && !req.getRoomId().isBlank()) {
            return List.of(req.getRoomId().trim());
        }
        throw new RuntimeException("roomIds is required");
    }

    private Client upsertClient(CreateBookingRequest.ClientInput input) {
        return clientRepository.findByEmail(input.getEmail())
                .map(existing -> {
                    existing.setFullName(input.getFullName());
                    existing.setPhone(input.getPhone());
                    existing.setAddress(input.getAddress());
                    existing.setDescription(input.getDescription());
                    existing.setIdCardNumber(input.getIdCardNumber());
                    return clientRepository.save(existing);
                })
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .idCardNumber(input.getIdCardNumber())
                        .fullName(input.getFullName())
                        .email(input.getEmail())
                        .phone(input.getPhone())
                        .address(input.getAddress())
                        .description(input.getDescription())
                        .build()));
    }
}
