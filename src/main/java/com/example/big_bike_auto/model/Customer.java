package com.example.big_bike_auto.model;

import java.util.ArrayList;
import java.util.List;

public class Customer {
    private String id;
    private String name;
    private String phone;
    private String plate;
    private String province;
    private List<Repair> repairs;

    public Customer(String id, String name, String phone, String plate, String province) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.plate = plate;
        this.province = province;
        this.repairs = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPlate() { return plate; }
    public String getProvince() { return province; }
    public List<Repair> getRepairs() { return repairs; }

    public void addRepair(Repair repair) {
        this.repairs.add(repair);
    }
}
