package com.example.omsv6.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.omsv6.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  @Autowired
  private PaymentService paymentService;


  @PostMapping("")
  public Map<String,Object> createPayment(@RequestBody Map<String,Object> request) {
    return paymentService.createPayment(request);
  }

  @GetMapping("/{id}")
  public Map<String,Object> getPaymentById(@PathVariable Long id) {
    return paymentService.getPaymentById(id);
  }

  @PostMapping("/{id}/capture")
  public Map<String,Object> capturePayment(@PathVariable Long id) {
    return paymentService.capturePayment(id);
  }

  @PostMapping("/{id}/refund")
  public Map<String,Object> refundPayment(@PathVariable Long id, @RequestBody Map<String,Object> request) {
    return paymentService.refundPayment(id, request);
  }
}
