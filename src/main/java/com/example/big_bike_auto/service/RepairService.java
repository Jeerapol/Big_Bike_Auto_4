package com.example.big_bike_auto.service;

import com.example.big_bike_auto.model.Customer;
import com.example.big_bike_auto.model.Repair;
import com.example.big_bike_auto.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;

public class RepairService {
    private final CustomerRepository customerRepo = new CustomerRepository();

    public List<Repair> getRepairsByCustomer(String customerId) {
        Optional<Customer> c = customerRepo.findAll().stream()
                .filter(x -> x.getId().equals(customerId))
                .findFirst();
        return c.map(Customer::getRepairs).orElse(List.of());
    }

    public void addRepair(String customerId, Repair repair) {
        List<Customer> customers = customerRepo.findAll();
        for (Customer c : customers) {
            if (c.getId().equals(customerId)) {
                c.addRepair(repair);
                break;
            }
        }
        customerRepo.saveAll(customers);
    }
}
