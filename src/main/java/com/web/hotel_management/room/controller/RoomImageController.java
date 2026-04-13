package com.web.hotel_management.room.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomImageController {

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable String id) {
        Path dir = Path.of("uploads", "rooms").toAbsolutePath().normalize();
        List<String> exts = List.of(".jpg", ".jpeg", ".png", ".webp");
        for (String ext : exts) {
            Path p = dir.resolve(id + ext);
            if (Files.exists(p)) {
                MediaType mt = switch (ext) {
                    case ".png" -> MediaType.IMAGE_PNG;
                    case ".webp" -> MediaType.parseMediaType("image/webp");
                    default -> MediaType.IMAGE_JPEG;
                };
                return ResponseEntity.ok()
                        .contentType(mt)
                        .body(new FileSystemResource(p));
            }
        }
        return ResponseEntity.notFound().build();
    }
}

