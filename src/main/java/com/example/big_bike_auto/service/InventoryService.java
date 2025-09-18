package com.example.big_bike_auto.service;

import com.example.big_bike_auto.model.Part;
import com.example.big_bike_auto.repository.PartRepository;
import java.util.List;

public class InventoryService {
    private final PartRepository partRepo = new PartRepository();

    public List<Part> getAllParts() {
        return partRepo.findAll();
    }

    public void updateInventory(List<Part> parts) {
        partRepo.saveAll(parts);
    }
}
