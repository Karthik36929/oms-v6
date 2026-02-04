package com.example.omsv6.service;

import com.example.omsv6.entity.InventoryItem;
import com.example.omsv6.entity.OrderEntity;
import com.example.omsv6.entity.PaymentEntity;
import com.example.omsv6.repository.InventoryItemRepository;
import com.example.omsv6.repository.OrderRepository;
import com.example.omsv6.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ReportService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public Map<String, Object> salesReport(String from, String to) {
        LocalDateTime fromDt = parseDateFrom(from);
        LocalDateTime toDt = parseDateTo(to);

        List<OrderEntity> orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toDt);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;
        Map<String, Integer> qtyBySku = new HashMap<>();
        Map<String, BigDecimal> amountBySku = new HashMap<>();

        for (OrderEntity o : orders) {
            if (o.getAmount() != null) {
                totalAmount = totalAmount.add(o.getAmount());
            }
            totalQty += o.getQuantity();
            String sku = o.getSku() != null ? o.getSku() : "";
            qtyBySku.put(sku, qtyBySku.getOrDefault(sku, 0) + o.getQuantity());
            BigDecimal prev = amountBySku.getOrDefault(sku, BigDecimal.ZERO);
            amountBySku.put(sku, prev.add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sales report generated");
        response.put("from", fromDt.toString());
        response.put("to", toDt.toString());
        response.put("orderCount", orders.size());
        response.put("totalQuantity", totalQty);
        response.put("totalAmount", totalAmount);
        response.put("quantityBySku", qtyBySku);
        response.put("amountBySku", amountBySku);
        return response;
    }

    public Map<String, Object> lowStockReport(int threshold) {
        int t = Math.max(0, threshold);
        List<InventoryItem> items = inventoryItemRepository.findAllByOrderBySkuAsc();
        List<Map<String, Object>> low = new ArrayList<>();

        for (InventoryItem i : items) {
            if (i.getQuantityAvailable() <= t) {
                Map<String, Object> m = new HashMap<>();
                m.put("sku", i.getSku());
                m.put("name", i.getName());
                m.put("quantityAvailable", i.getQuantityAvailable());
                m.put("quantityReserved", i.getQuantityReserved());
                low.add(m);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Low stock report generated");
        response.put("threshold", t);
        response.put("count", low.size());
        response.put("items", low);
        return response;
    }

    public Map<String, Object> paymentSummary(String from, String to) {
        LocalDateTime fromDt = parseDateFrom(from);
        LocalDateTime toDt = parseDateTo(to);

        List<PaymentEntity> payments = paymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toDt);

        BigDecimal authorized = BigDecimal.ZERO;
        BigDecimal captured = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        Map<String, Integer> countByStatus = new HashMap<>();

        for (PaymentEntity p : payments) {
            String st = p.getStatus() != null ? p.getStatus() : "UNKNOWN";
            countByStatus.put(st, countByStatus.getOrDefault(st, 0) + 1);
            BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            if ("AUTHORIZED".equalsIgnoreCase(st)) authorized = authorized.add(amt);
            if ("CAPTURED".equalsIgnoreCase(st)) captured = captured.add(amt);
            if ("REFUNDED".equalsIgnoreCase(st)) refunded = refunded.add(amt);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Payment summary generated");
        response.put("from", fromDt.toString());
        response.put("to", toDt.toString());
        response.put("paymentCount", payments.size());
        response.put("countByStatus", countByStatus);
        response.put("totalAuthorized", authorized);
        response.put("totalCaptured", captured);
        response.put("totalRefunded", refunded);
        return response;
    }

    private LocalDateTime parseDateFrom(String s) {
        if (s == null || s.isBlank()) {
            return LocalDate.now().minusDays(30).atStartOfDay();
        }
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (DateTimeParseException ex) {
            return LocalDate.now().minusDays(30).atStartOfDay();
        }
    }

    private LocalDateTime parseDateTo(String s) {
        if (s == null || s.isBlank()) {
            return LocalDate.now().plusDays(1).atStartOfDay();
        }
        try {
            return LocalDate.parse(s).plusDays(1).atStartOfDay();
        } catch (DateTimeParseException ex) {
            return LocalDate.now().plusDays(1).atStartOfDay();
        }
    }
}
