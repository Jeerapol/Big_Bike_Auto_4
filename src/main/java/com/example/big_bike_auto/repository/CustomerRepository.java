package com.example.big_bike_auto.repository;

import com.example.big_bike_auto.model.Customer;
import com.example.big_bike_auto.common.JsonUtil;
import java.util.List;

public class CustomerRepository {
    private static final String FILE_PATH = "data/customers.json";

    public List<Customer> findAll() {
        return JsonUtil.readList(FILE_PATH, Customer.class);
    }

    public void saveAll(List<Customer> customers) {
        JsonUtil.writeList(FILE_PATH, customers);
    }
}
