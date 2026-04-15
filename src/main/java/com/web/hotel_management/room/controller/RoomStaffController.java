package com.web.hotel_management.room.controller;

import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import com.web.hotel_management.booking.repository.BookedRoomRepository;
import com.web.hotel_management.room.dto.RoomStaffRequest;
import com.web.hotel_management.room.dto.RoomStaffResponse;
import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.repository.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/staff/rooms")
public class RoomStaffController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookedRoomRepository bookedRoomRepository;

    public RoomStaffController(RoomRepository roomRepository, HotelRepository hotelRepository, BookedRoomRepository bookedRoomRepository) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.bookedRoomRepository = bookedRoomRepository;
    }

    @GetMapping
    public List<RoomStaffResponse> list(
            @RequestParam(required = false) Integer hotelId,
            @RequestParam(required = false) String type
    ) {
        // keep it simple for staff UI
        return roomRepository.search(hotelId, normalize(type), null, null)
                .stream()
                .map(RoomStaffResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public RoomStaffResponse get(@PathVariable String id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        return RoomStaffResponse.fromEntity(room);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomStaffResponse create(@Valid @RequestBody RoomStaffRequest req) {
        String id = req.getId() != null ? req.getId().trim() : null;
        if (id == null || id.isBlank()) {
            throw new RuntimeException("Room id is required");
        }
        if (roomRepository.existsById(id)) {
            throw new RuntimeException("Room id already exists: " + id);
        }

        Hotel hotel = hotelRepository.findById(req.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + req.getHotelId()));

        Room room = Room.builder()
                .id(id)
                .name(req.getName().trim())
                .type(normalizeRoomType(req.getType()))
                .price(req.getPrice())
                .description(req.getDescription())
                .hotel(hotel)
                .build();
        return RoomStaffResponse.fromEntity(roomRepository.save(room));
    }

    @PutMapping("/{id}")
    public RoomStaffResponse update(@PathVariable String id, @Valid @RequestBody RoomStaffRequest req) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));

        Hotel hotel = hotelRepository.findById(req.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + req.getHotelId()));

        room.setName(req.getName().trim());
        room.setType(normalizeRoomType(req.getType()));
        room.setPrice(req.getPrice());
        room.setDescription(req.getDescription());
        room.setHotel(hotel);

        return RoomStaffResponse.fromEntity(roomRepository.save(room));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Room not found with id: " + id);
        }
        if (!bookedRoomRepository.findByRoom_Id(id).isEmpty()) {
            throw new RuntimeException("Cannot delete room because it has booked rooms");
        }
        roomRepository.deleteById(id);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoomStaffResponse uploadImage(@PathVariable String id, @RequestPart("file") MultipartFile file) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        String ct = file.getContentType();
        String ext = contentTypeToExt(ct);
        if (ext == null) {
            throw new RuntimeException("Only JPG/PNG/WEBP/GIF/SVG images are allowed");
        }

        try {
            Path dir = Path.of("uploads", "rooms").toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // delete old versions (same id different ext)
            deleteIfExists(dir.resolve(id + ".jpg"));
            deleteIfExists(dir.resolve(id + ".jpeg"));
            deleteIfExists(dir.resolve(id + ".png"));
            deleteIfExists(dir.resolve(id + ".webp"));
            deleteIfExists(dir.resolve(id + ".gif"));
            deleteIfExists(dir.resolve(id + ".svg"));

            Path out = dir.resolve(id + ext);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }

        return RoomStaffResponse.fromEntity(room);
    }

    private static void deleteIfExists(Path p) throws IOException {
        if (Files.exists(p)) Files.delete(p);
    }

    private static String contentTypeToExt(String ct) {
        if (ct == null) return null;
        return switch (ct.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/svg+xml" -> ".svg";
            default -> null;
        };
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Canonical type after validation: Standard | Deluxe | Suite */
    private static String normalizeRoomType(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        for (String opt : List.of("Standard", "Deluxe", "Suite")) {
            if (opt.equalsIgnoreCase(t)) {
                return opt;
            }
        }
        return t;
    }
}

