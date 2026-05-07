package com.web.hotel_management.roomtype.controller;

import com.web.hotel_management.roomtype.dto.RoomTypeDto;
import com.web.hotel_management.roomtype.dto.RoomTypeUpsertRequest;
import com.web.hotel_management.roomtype.entity.RoomType;
import com.web.hotel_management.roomtype.entity.RoomTypeStatus;
import com.web.hotel_management.roomtype.repository.RoomTypeRepository;
import com.web.hotel_management.activity.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/room-types")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomTypeController {
    private final RoomTypeRepository roomTypeRepository;
    private final ActivityLogService activityLogService;

    public AdminRoomTypeController(RoomTypeRepository roomTypeRepository, ActivityLogService activityLogService) {
        this.roomTypeRepository = roomTypeRepository;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public List<RoomTypeDto> list() {
        return roomTypeRepository.findAll().stream().map(RoomTypeDto::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public RoomTypeDto get(@PathVariable Integer id) {
        RoomType rt = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RoomType not found with id: " + id));
        return RoomTypeDto.fromEntity(rt);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeDto create(@Valid @RequestBody RoomTypeUpsertRequest req, Authentication authentication) {
        String name = req.getName() != null ? req.getName().trim() : "";
        if (name.isBlank()) throw new RuntimeException("name is required");
        String code = req.getCode() != null ? req.getCode().trim() : null;
        if (code == null || code.isBlank()) throw new RuntimeException("code is required");
        if (roomTypeRepository.existsByCodeIgnoreCase(code)) throw new RuntimeException("RoomType code already exists: " + code);
        if (roomTypeRepository.existsByNameIgnoreCase(name)) throw new RuntimeException("RoomType name already exists: " + name);

        RoomTypeStatus st = req.getStatus() != null ? req.getStatus() : RoomTypeStatus.ACTIVE;
        RoomType rt = RoomType.builder()
                .code(code)
                .name(name)
                .description(req.getDescription())
                .basePrice(req.getBasePrice())
                .status(st)
                .build();
        RoomType saved = roomTypeRepository.save(rt);
        log.info("[ADMIN] create room type: id={}, code={}, name={}", saved.getId(), saved.getCode(), saved.getName());
        activityLogService.log(authentication, "ADMIN_ROOMTYPE_CREATE", "ROOM_TYPE", String.valueOf(saved.getId()),
                "code=" + saved.getCode() + ",name=" + saved.getName() + ",price=" + saved.getBasePrice());
        return RoomTypeDto.fromEntity(saved);
    }

    @PutMapping("/{id}")
    public RoomTypeDto update(@PathVariable Integer id, @Valid @RequestBody RoomTypeUpsertRequest req, Authentication authentication) {
        RoomType rt = roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RoomType not found with id: " + id));

        String name = req.getName() != null ? req.getName().trim() : "";
        if (name.isBlank()) throw new RuntimeException("name is required");
        String code = req.getCode() != null ? req.getCode().trim() : null;
        if (code == null || code.isBlank()) throw new RuntimeException("code is required");
        roomTypeRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("RoomType code already exists: " + code);
            }
        });
        if (!name.equalsIgnoreCase(rt.getName()) && roomTypeRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("RoomType name already exists: " + name);
        }

        rt.setCode(code);
        rt.setName(name);
        rt.setDescription(req.getDescription());
        rt.setBasePrice(req.getBasePrice());
        rt.setStatus(req.getStatus() != null ? req.getStatus() : rt.getStatus());
        RoomType saved = roomTypeRepository.save(rt);
        log.info("[ADMIN] update room type: id={}, code={}, name={}", saved.getId(), saved.getCode(), saved.getName());
        activityLogService.log(authentication, "ADMIN_ROOMTYPE_UPDATE", "ROOM_TYPE", String.valueOf(saved.getId()),
                "code=" + saved.getCode() + ",name=" + saved.getName() + ",price=" + saved.getBasePrice());
        return RoomTypeDto.fromEntity(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id, Authentication authentication) {
        if (!roomTypeRepository.existsById(id)) throw new RuntimeException("RoomType not found with id: " + id);
        roomTypeRepository.deleteById(id);
        log.info("[ADMIN] delete room type: id={}", id);
        activityLogService.log(authentication, "ADMIN_ROOMTYPE_DELETE", "ROOM_TYPE", String.valueOf(id), "deleted");
    }
}

