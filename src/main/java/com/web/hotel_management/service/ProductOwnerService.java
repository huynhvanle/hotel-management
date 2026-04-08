package com.web.hotel_management.service;

import com.web.hotel_management.entity.ProductOwner;
import com.web.hotel_management.repository.ProductOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductOwnerService {

    @Autowired
    private ProductOwnerRepository productOwnerRepository;

    public List<ProductOwner> getAllOwners() {
        return productOwnerRepository.findAll();
    }

    public ProductOwner saveOwner(ProductOwner owner) {
        return productOwnerRepository.save(owner);
    }
}