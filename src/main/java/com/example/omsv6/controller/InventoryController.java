package com.example.omsv6.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.omsv6.service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

  @Autowired
  private InventoryService inventoryService;


  @PostMapping("/items")
  public Map<String,Object> createItem(@RequestBody Map<String,Object> request) {
    return inventoryService.createItem(request);
  }

  @GetMapping("/items/{sku}")
  public Map<String,Object> getItemBySku(@PathVariable String sku) {
    return inventoryService.getItemBySku(sku);
  }

  @PutMapping("/items/{sku}/adjust")
  public Map<String,Object> adjustStock(@PathVariable String sku, @RequestBody Map<String,Object> request) {
    return inventoryService.adjustStock(sku, request);
  }

  @GetMapping("/items")
  public Map<String,Object> listItems(@RequestParam int lowStockThreshold) {
    return inventoryService.listItems(lowStockThreshold);
  }
}
