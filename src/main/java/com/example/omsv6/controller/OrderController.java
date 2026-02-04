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
import com.example.omsv6.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  @Autowired
  private OrderService orderService;


  @PostMapping("")
  public Map<String,Object> createOrder(@RequestBody Map<String,Object> request) {
    return orderService.createOrder(request);
  }

  @GetMapping("/{id}")
  public Map<String,Object> getOrderById(@PathVariable Long id) {
    return orderService.getOrderById(id);
  }

  @GetMapping("")
  public Map<String,Object> listOrders(@RequestParam String status, @RequestParam String customerId) {
    return orderService.listOrders(status, customerId);
  }

  @PutMapping("/{id}/status")
  public Map<String,Object> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String,Object> request) {
    return orderService.updateOrderStatus(id, request);
  }

  @DeleteMapping("/{id}")
  public Map<String,Object> cancelOrder(@PathVariable Long id) {
    return orderService.cancelOrder(id);
  }
}
