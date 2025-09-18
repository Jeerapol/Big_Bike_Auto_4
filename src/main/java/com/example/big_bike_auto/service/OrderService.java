package com.example.big_bike_auto.service;

import com.example.big_bike_auto.model.Part;
import com.example.big_bike_auto.model.PurchaseOrder;
import com.example.big_bike_auto.repository.PurchaseOrderRepository;
import java.util.List;

public class OrderService {
    private final PurchaseOrderRepository orderRepo = new PurchaseOrderRepository();

    public List<PurchaseOrder> getAllOrders() {
        return orderRepo.findAll();
    }

    public void createOrder(PurchaseOrder order) {
        List<PurchaseOrder> orders = orderRepo.findAll();
        orders.add(order);
        orderRepo.saveAll(orders);
    }

    public void markOrderReceived(String orderId) {
        List<PurchaseOrder> orders = orderRepo.findAll();
        for (PurchaseOrder o : orders) {
            if (o.getId().equals(orderId)) {
                o.markAsReceived();
            }
        }
        orderRepo.saveAll(orders);
    }
}
