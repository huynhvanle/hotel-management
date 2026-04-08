package com.web.hotel_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "ProductOwner")
@Data
public class ProductOwner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String phoneNumber;
    private String email;

    @OneToMany(mappedBy = "productOwner", cascade = CascadeType.ALL)
    private List<Hotel> hotels;
}