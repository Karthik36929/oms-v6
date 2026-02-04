package com.example.omsv6.service;

import com.example.omsv6.entity.InventoryItem;
import com.example.omsv6.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class InventoryService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Transactional
    public Map<String, Object> createItem(Map<String, Object> request) {
        Object skuObj = request.get("sku");
        String sku = skuObj != null ? skuObj.toString() : "";

        Object nameObj = request.get("name");
        String name = nameObj != null ? nameObj.toString() : "";

        Object qtyObj = request.get("quantityAvailable");
        int qty = safeInt(qtyObj, 0);

        Map<String, Object> response = new HashMap<>();
        if (sku.isBlank()) {
            response.put("message", "Validation failed");
            response.put("error", "sku is required");
            return response;
        }

        Optional<InventoryItem> existing = inventoryItemRepository.findBySku(sku);
        if (existing.isPresent()) {
            response.put("message", "Item already exists");
            response.put("sku", sku);
            return response;
        }

        InventoryItem item = new InventoryItem();
        item.setSku(sku);
        item.setName(name);
        item.setQuantityAvailable(Math.max(0, qty));
        item.setQuantityReserved(0);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item = inventoryItemRepository.save(item);

        response.put("message", "Inventory item created");
        response.put("itemId", item.getId());
        response.put("sku", item.getSku());
        response.put("quantityAvailable", item.getQuantityAvailable());
        return response;
    }

    public Map<String, Object> getItemBySku(String sku) {
        Map<String, Object> response = new HashMap<>();
        String s = sku != null ? sku : "";
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findBySku(s);
        if (itemOpt.isEmpty()) {
            response.put("message", "Inventory item not found");
            response.put("sku", s);
            return response;
        }
        InventoryItem item = itemOpt.get();
        response.put("message", "Inventory item retrieved");
        response.put("item", toItemMap(item));
        return response;
    }

    @Transactional
    public Map<String, Object> adjustStock(String sku, Map<String, Object> request) {
        Object deltaObj = request.get("delta");
        int delta = safeInt(deltaObj, 0);

        Map<String, Object> response = new HashMap<>();
        String s = sku != null ? sku : "";
        Optional<InventoryItem> itemOpt = inventoryItemRepository.findBySku(s);
        if (itemOpt.isEmpty()) {
            response.put("message", "Inventory item not found");
            response.put("sku", s);
            return response;
        }

        InventoryItem item = itemOpt.get();
        int newAvailable = item.getQuantityAvailable() + delta;
        if (newAvailable < 0) {
            response.put("message", "Insufficient available stock for adjustment");
            response.put("sku", s);
            response.put("available", item.getQuantityAvailable());
            response.put("delta", delta);
            return response;
        }

        item.setQuantityAvailable(newAvailable);
        item.setUpdatedAt(LocalDateTime.now());
        inventoryItemRepository.save(item);

        response.put("message", "Stock adjusted");
        response.put("sku", s);
        response.put("quantityAvailable", item.getQuantityAvailable());
        response.put("quantityReserved", item.getQuantityReserved());
        return response;
    }

    public Map<String, Object> listItems(int lowStockThreshold) {
        Map<String, Object> response = new HashMap<>();
        int threshold = Math.max(0, lowStockThreshold);

        List<InventoryItem> items = inventoryItemRepository.findAllByOrderBySkuAsc();
        List<Map<String, Object>> list = new ArrayList<>();
        List<Map<String, Object>> lowStock = new ArrayList<>();

        for (InventoryItem i : items) {
            Map<String, Object> m = toItemMap(i);
            list.add(m);
            if (i.getQuantityAvailable() <= threshold) {
                lowStock.add(m);
            }
        }

        response.put("message", "Inventory items listed");
        response.put("count", list.size());
        response.put("items", list);
        response.put("lowStockThreshold", threshold);
        response.put("lowStockCount", lowStock.size());
        response.put("lowStockItems", lowStock);
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

    private Map<String, Object> toItemMap(InventoryItem i) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", i.getId());
        m.put("sku", i.getSku());
        m.put("name", i.getName());
        m.put("quantityAvailable", i.getQuantityAvailable());
        m.put("quantityReserved", i.getQuantityReserved());
        m.put("createdAt", i.getCreatedAt() != null ? i.getCreatedAt().toString() : null);
        m.put("updatedAt", i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null);
        return m;
    }
}
