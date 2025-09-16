package com.example.big_bike_auto.customer;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Customer model สำหรับเก็บข้อมูลผู้ลงทะเบียน
 * - id: UUID ในรูป string
 * - name/phone/email: ข้อมูลติดต่อ
 * - registeredAt: วันที่และเวลาที่ลงทะเบียน
 */
public class Customer {
    private String id;
    private String name;
    private String phone;
    private String email;
    private LocalDateTime registeredAt;

    public Customer() {}

    public Customer(String id, String name, String phone, String email, LocalDateTime registeredAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer that = (Customer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
