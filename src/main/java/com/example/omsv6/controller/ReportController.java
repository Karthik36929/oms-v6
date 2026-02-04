package com.example.omsv6.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.omsv6.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

  @Autowired
  private ReportService reportService;


  @GetMapping("/sales")
  public Map<String,Object> salesReport(@RequestParam String from, @RequestParam String to) {
    return reportService.salesReport(from, to);
  }

  @GetMapping("/inventory/low-stock")
  public Map<String,Object> lowStockReport(@RequestParam int threshold) {
    return reportService.lowStockReport(threshold);
  }

  @GetMapping("/payments/summary")
  public Map<String,Object> paymentSummary(@RequestParam String from, @RequestParam String to) {
    return reportService.paymentSummary(from, to);
  }
}
