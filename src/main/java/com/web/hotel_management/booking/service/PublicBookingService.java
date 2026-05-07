package com.web.hotel_management.booking.service;

import com.web.hotel_management.booking.dto.BookingResponse;
import com.web.hotel_management.booking.dto.CreateBookingRequest;
import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingRoom;
import com.web.hotel_management.booking.entity.BookingStatus;
import com.web.hotel_management.booking.repository.BookingRepository;
import com.web.hotel_management.booking.repository.BookingRoomRepository;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.client.repository.ClientRepository;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class PublicBookingService {
    private final BookingRepository bookingRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final ClientRepository clientRepository;
    private final RoomRepository roomRepository;
    private final BookingPricingHelper bookingPricingHelper;

    public PublicBookingService(
            BookingRepository bookingRepository,
            BookingRoomRepository bookingRoomRepository,
            ClientRepository clientRepository,
            RoomRepository roomRepository,
            BookingPricingHelper bookingPricingHelper
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.clientRepository = clientRepository;
        this.roomRepository = roomRepository;
        this.bookingPricingHelper = bookingPricingHelper;
    }

    public BookingResponse createPublicBooking(CreateBookingRequest req) {
        Client client = resolveAuthenticatedClient(req.getClient());

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

        for (String roomId : roomIds) {
            if (bookingRoomRepository.existsOverlappingActiveBooking(roomId, req.getCheckin(), req.getCheckout())) {
                throw new RuntimeException("Room is already booked for selected dates: " + roomId);
            }
        }

        // BR2: chốt giá theo RoomType tại thời điểm tạo booking.
        double total = bookingPricingHelper.stayTotal(rooms, req.getCheckin(), req.getCheckout());
        double deposit = bookingPricingHelper.depositFromTotal(total);

        Booking booking = Booking.builder()
                .checkin(req.getCheckin())
                .checkout(req.getCheckout())
                .status(BookingStatus.PENDING)
                .depositAmount(deposit)
                .client(client)
                .build();
        Booking savedBooking = bookingRepository.save(booking);

        Integer firstBookingRoomId = null;
        for (Room room : rooms) {
            BookingRoom br = BookingRoom.builder()
                    .booking(savedBooking)
                    .room(room)
                    .unitPrice(room.getRoomType() != null ? room.getRoomType().getBasePrice() : null)
                    .build();
            BookingRoom saved = bookingRoomRepository.save(br);
            if (firstBookingRoomId == null) firstBookingRoomId = saved.getId();
        }

        return BookingResponse.builder()
                .bookingId(savedBooking.getId())
                .bookedRoomId(firstBookingRoomId)
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

    private Client resolveAuthenticatedClient(CreateBookingRequest.ClientInput inputMaybe) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Client must be authenticated to book.");
        }
        String phone = auth.getName();
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Invalid client identity.");
        }
        Client client = clientRepository.findByPhone(phone.trim())
                .orElseThrow(() -> new RuntimeException("Client profile not found for: " + phone));

        // Optional: allow client to update profile fields via booking form (not phone — tied to login).
        if (inputMaybe != null) {
            if (inputMaybe.getFullName() != null && !inputMaybe.getFullName().isBlank()) client.setFullName(inputMaybe.getFullName());
            if (inputMaybe.getIdCardNumber() != null) client.setIdCardNumber(inputMaybe.getIdCardNumber());
            client = clientRepository.save(client);
        }
        return client;
    }
}
