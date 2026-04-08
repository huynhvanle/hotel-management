package com.web.hotel_management.controller;

import com.web.hotel_management.entity.ProductOwner;
import com.web.hotel_management.service.ProductOwnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/owners")
public class ProductOwnerController {
    @Autowired
    private ProductOwnerService ownerService;

    @GetMapping
    public ResponseEntity<List<ProductOwner>> getAllOwners() {
        return ResponseEntity.ok(ownerService.getAllOwners());
    }

    @PostMapping
    public ResponseEntity<ProductOwner> createOwner(@RequestBody ProductOwner owner) {
        return ResponseEntity.ok(ownerService.saveOwner(owner));
    }
}