package com.example.omsv6.service;

import com.example.omsv6.entity.InventoryItem;
import com.example.omsv6.entity.OrderEntity;
import com.example.omsv6.entity.PaymentEntity;
import com.example.omsv6.repository.InventoryItemRepository;
import com.example.omsv6.repository.OrderRepository;
import com.example.omsv6.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ExternalApiClient externalApiClient;

    @Transactional
    public Map<String, Object> createOrder(Map<String, Object> request) {
        Object customerIdObj = request.get("customerId");
        String customerId = customerIdObj != null ? customerIdObj.toString() : "";

        Object skuObj = request.get("sku");
        String sku = skuObj != null ? skuObj.toString() : "";

        Object quantityObj = request.get("quantity");
        int quantity = safeInt(quantityObj, 1);

        Object amountObj = request.get("amount");
        BigDecimal amount = safeBigDecimal(amountObj, "0");

        Map<String, Object> response = new HashMap<>();
        if (customerId.isBlank() || sku.isBlank()) {
            response.put("message", "Validation failed");
            response.put("error", "customerId and sku are required");
            return response;
        }
        if (quantity <= 0) {
            response.put("message", "Validation failed");
            response.put("error", "quantity must be > 0");
            return response;
        }

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findBySku(sku);
        if (itemOpt.isEmpty()) {
            response.put("message", "Inventory item not found");
            response.put("sku", sku);
            return response;
        }

        InventoryItem item = itemOpt.get();
        if (item.getQuantityAvailable() < quantity) {
            response.put("message", "Insufficient stock");
            response.put("sku", sku);
            response.put("available", item.getQuantityAvailable());
            response.put("requested", quantity);
            return response;
        }

        // Reserve inventory
        item.setQuantityAvailable(item.getQuantityAvailable() - quantity);
        item.setQuantityReserved(item.getQuantityReserved() + quantity);
        inventoryItemRepository.save(item);

        OrderEntity order = new OrderEntity();
        order.setCustomerId(customerId);
        order.setSku(sku);
        order.setQuantity(quantity);
        order.setAmount(amount);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        // External integration (e.g., shipping quote)
        Map<String, Object> shippingQuote = externalApiClient.fetchShippingQuote(customerId, sku, quantity);

        response.put("message", "Order created");
        response.put("orderId", order.getId());
        response.put("status", order.getStatus());
        response.put("inventory", Map.of("sku", sku, "reserved", quantity));
        response.put("external", shippingQuote);
        return response;
    }

    public Map<String, Object> getOrderById(Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<OrderEntity> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            response.put("message", "Order not found");
            response.put("orderId", id);
            return response;
        }
        OrderEntity o = orderOpt.get();
        response.put("message", "Order retrieved");
        response.put("order", toOrderMap(o));
        return response;
    }

    public Map<String, Object> listOrders(String status, String customerId) {
        Map<String, Object> response = new HashMap<>();
        String st = status != null ? status : "";
        String cid = customerId != null ? customerId : "";

        List<OrderEntity> orders;
        if (!st.isBlank() && !cid.isBlank()) {
            orders = orderRepository.findByStatusAndCustomerIdOrderByCreatedAtDesc(st, cid);
        } else if (!st.isBlank()) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(st);
        } else if (!cid.isBlank()) {
            orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(cid);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderEntity o : orders) {
            list.add(toOrderMap(o));
        }

        response.put("message", "Orders listed");
        response.put("count", list.size());
        response.put("orders", list);
        return response;
    }

    @Transactional
    public Map<String, Object> updateOrderStatus(Long id, Map<String, Object> request) {
        Object statusObj = request.get("status");
        String newStatus = statusObj != null ? statusObj.toString() : "";

        Map<String, Object> response = new HashMap<>();
        if (newStatus.isBlank()) {
            response.put("message", "Validation failed");
            response.put("error", "status is required");
            return response;
        }

        Optional<OrderEntity> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            response.put("message", "Order not found");
            response.put("orderId", id);
            return response;
        }

        OrderEntity order = orderOpt.get();
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        response.put("message", "Order status updated");
        response.put("orderId", order.getId());
        response.put("oldStatus", oldStatus);
        response.put("newStatus", newStatus);
        return response;
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<OrderEntity> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            response.put("message", "Order not found");
            response.put("orderId", id);
            return response;
        }

        OrderEntity order = orderOpt.get();
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            response.put("message", "Order already cancelled");
            response.put("orderId", id);
            return response;
        }

        // Release inventory reservation
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findBySku(order.getSku());
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            int qty = order.getQuantity();
            item.setQuantityReserved(Math.max(0, item.getQuantityReserved() - qty));
            item.setQuantityAvailable(item.getQuantityAvailable() + qty);
            inventoryItemRepository.save(item);
        }

        // Mark related payment as CANCELLED if exists and not captured
        List<PaymentEntity> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        for (PaymentEntity p : payments) {
            if (!"CAPTURED".equalsIgnoreCase(p.getStatus()) && !"REFUNDED".equalsIgnoreCase(p.getStatus())) {
                p.setStatus("CANCELLED");
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
            }
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        response.put("message", "Order cancelled");
        response.put("orderId", id);
        response.put("status", "CANCELLED");
        return response;
    }

    private int safeInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal safeBigDecimal(Object obj, String defaultValue) {
        if (obj == null) return new BigDecimal(defaultValue);
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return new BigDecimal(defaultValue);
        }
    }

    private Map<String, Object> toOrderMap(OrderEntity o) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("customerId", o.getCustomerId());
        m.put("sku", o.getSku());
        m.put("quantity", o.getQuantity());
        m.put("amount", o.getAmount());
        m.put("status", o.getStatus());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);
        return m;
    }
}
