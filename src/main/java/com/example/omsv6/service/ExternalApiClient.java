package com.example.omsv6.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ExternalApiClient {

    private final RestTemplate restTemplate;

    @Value("${external.api.base-url:https://httpbin.org}")
    private String baseUrl;

    @Value("${external.api.timeout-ms:2000}")
    private String timeoutMs;

    public ExternalApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> fetchShippingQuote(String customerId, String sku, int quantity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customerId);
        payload.put("sku", sku);
        payload.put("quantity", quantity);
        payload.put("requestedAt", Instant.now().toString());

        Map<String, Object> result = postToExternal("/post", payload);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "External shipping quote fetched");
        response.put("provider", "httpbin");
        response.put("echo", result);
        response.put("quote", Map.of("currency", "USD", "amount", "5.99"));
        return response;
    }

    public String authorizePayment(Long orderId, BigDecimal amount, String currency) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("amount", amount);
        payload.put("currency", currency);
        payload.put("action", "AUTHORIZE");
        payload.put("requestedAt", Instant.now().toString());

        postToExternal("/post", payload);
        return "AUTH-" + orderId + "-" + System.currentTimeMillis();
    }

    public boolean capturePayment(String externalReference) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("externalReference", externalReference);
        payload.put("action", "CAPTURE");
        payload.put("requestedAt", Instant.now().toString());

        Map<String, Object> result = postToExternal("/post", payload);
        return result != null;
    }

    public boolean refundPayment(String externalReference, BigDecimal amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("externalReference", externalReference);
        payload.put("amount", amount);
        payload.put("action", "REFUND");
        payload.put("requestedAt", Instant.now().toString());

        Map<String, Object> result = postToExternal("/post", payload);
        return result != null;
    }

    private Map<String, Object> postToExternal(String path, Map<String, Object> payload) {
        try {
            String url = baseUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> body = resp.getBody() != null ? resp.getBody() : new HashMap<>();
            Map<String, Object> out = new HashMap<>();
            out.put("httpStatus", resp.getStatusCode().value());
            out.put("body", body);
            return out;
        } catch (Exception e) {
            Map<String, Object> out = new HashMap<>();
            out.put("httpStatus", 0);
            out.put("error", e.getClass().getSimpleName());
            out.put("message", e.getMessage());
            return out;
        }
    }
}
