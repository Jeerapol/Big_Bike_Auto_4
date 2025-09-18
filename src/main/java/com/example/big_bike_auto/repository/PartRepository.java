package com.example.big_bike_auto.repository;

import com.example.big_bike_auto.model.Part;
import com.example.big_bike_auto.common.JsonUtil;
import java.util.List;

public class PartRepository {
    private static final String FILE_PATH = "data/parts.json";

    public List<Part> findAll() {
        return JsonUtil.readList(FILE_PATH, Part.class);
    }

    public void saveAll(List<Part> parts) {
        JsonUtil.writeList(FILE_PATH, parts);
    }
}
