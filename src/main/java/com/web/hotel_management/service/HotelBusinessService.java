package com.web.hotel_management.service;

import com.web.hotel_management.dto.HotelResponse;
import com.web.hotel_management.entity.Hotel;
import com.web.hotel_management.repository.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelBusinessService {

    @Autowired
    private HotelRepository hotelRepository;

    public List<HotelResponse> getAllHotels() {
        return hotelRepository.findAll().stream().map(hotel -> {
            HotelResponse res = new HotelResponse();
            res.setId(hotel.getId());
            res.setName(hotel.getName());
            res.setAddress(hotel.getAddress());
            res.setStarLevel(hotel.getStarLevel());
            if (hotel.getProductOwner() != null) {
                res.setOwnerName(hotel.getProductOwner().getName());
            }
            return res;
        }).collect(Collectors.toList());
    }

    public Hotel getHotelById(Integer id) {
        return hotelRepository.findById(id).orElse(null);
    }
}