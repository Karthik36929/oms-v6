package com.example.omsv6.service;

import com.example.omsv6.entity.OrderEntity;
import com.example.omsv6.entity.PaymentEntity;
import com.example.omsv6.repository.OrderRepository;
import com.example.omsv6.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ExternalApiClient externalApiClient;

    @Transactional
    public Map<String, Object> createPayment(Map<String, Object> request) {
        Object orderIdObj = request.get("orderId");
        Long orderId = safeLong(orderIdObj);

        Object amountObj = request.get("amount");
        BigDecimal amount = safeBigDecimal(amountObj, "0");

        Object currencyObj = request.get("currency");
        String currency = currencyObj != null ? currencyObj.toString() : "USD";

        Map<String, Object> response = new HashMap<>();
        if (orderId == null) {
            response.put("message", "Validation failed");
            response.put("error", "orderId is required");
            return response;
        }

        Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            response.put("message", "Order not found");
            response.put("orderId", orderId);
            return response;
        }

        OrderEntity order = orderOpt.get();
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            response.put("message", "Cannot pay for cancelled order");
            response.put("orderId", orderId);
            return response;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
        }

        String externalRef = externalApiClient.authorizePayment(orderId, amount, currency);

        PaymentEntity p = new PaymentEntity();
        p.setOrderId(orderId);
        p.setAmount(amount);
        p.setCurrency(currency);
        p.setProvider("EXTERNAL_SIM");
        p.setExternalReference(externalRef);
        p.setStatus("AUTHORIZED");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p = paymentRepository.save(p);

        response.put("message", "Payment authorized");
        response.put("paymentId", p.getId());
        response.put("orderId", orderId);
        response.put("status", p.getStatus());
        response.put("externalReference", p.getExternalReference());
        return response;
    }

    public Map<String, Object> getPaymentById(Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<PaymentEntity> pOpt = paymentRepository.findById(id);
        if (pOpt.isEmpty()) {
            response.put("message", "Payment not found");
            response.put("paymentId", id);
            return response;
        }

        PaymentEntity p = pOpt.get();
        response.put("message", "Payment retrieved");
        response.put("payment", toPaymentMap(p));
        return response;
    }

    @Transactional
    public Map<String, Object> capturePayment(Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<PaymentEntity> pOpt = paymentRepository.findById(id);
        if (pOpt.isEmpty()) {
            response.put("message", "Payment not found");
            response.put("paymentId", id);
            return response;
        }

        PaymentEntity p = pOpt.get();
        if (!"AUTHORIZED".equalsIgnoreCase(p.getStatus())) {
            response.put("message", "Payment not in AUTHORIZED state");
            response.put("paymentId", id);
            response.put("status", p.getStatus());
            return response;
        }

        boolean captured = externalApiClient.capturePayment(p.getExternalReference());
        if (!captured) {
            response.put("message", "External capture failed");
            response.put("paymentId", id);
            return response;
        }

        p.setStatus("CAPTURED");
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(p);

        // Move order forward if needed
        orderRepository.findById(p.getOrderId()).ifPresent(o -> {
            if (!"CANCELLED".equalsIgnoreCase(o.getStatus())) {
                o.setStatus("PAID");
                o.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(o);
            }
        });

        response.put("message", "Payment captured");
        response.put("paymentId", id);
        response.put("status", "CAPTURED");
        return response;
    }

    @Transactional
    public Map<String, Object> refundPayment(Long id, Map<String, Object> request) {
        Object amountObj = request.get("amount");
        BigDecimal refundAmount = safeBigDecimal(amountObj, "0");

        Map<String, Object> response = new HashMap<>();
        Optional<PaymentEntity> pOpt = paymentRepository.findById(id);
        if (pOpt.isEmpty()) {
            response.put("message", "Payment not found");
            response.put("paymentId", id);
            return response;
        }

        PaymentEntity p = pOpt.get();
        if (!"CAPTURED".equalsIgnoreCase(p.getStatus())) {
            response.put("message", "Only CAPTURED payments can be refunded");
            response.put("paymentId", id);
            response.put("status", p.getStatus());
            return response;
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            refundAmount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
        }

        boolean refunded = externalApiClient.refundPayment(p.getExternalReference(), refundAmount);
        if (!refunded) {
            response.put("message", "External refund failed");
            response.put("paymentId", id);
            return response;
        }

        p.setStatus("REFUNDED");
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(p);

        response.put("message", "Payment refunded");
        response.put("paymentId", id);
        response.put("status", "REFUNDED");
        response.put("amount", refundAmount);
        return response;
    }

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return null;
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

    private Map<String, Object> toPaymentMap(PaymentEntity p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("orderId", p.getOrderId());
        m.put("amount", p.getAmount());
        m.put("currency", p.getCurrency());
        m.put("provider", p.getProvider());
        m.put("externalReference", p.getExternalReference());
        m.put("status", p.getStatus());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return m;
    }
}
