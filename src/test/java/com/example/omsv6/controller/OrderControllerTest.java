package com.example.omsv6.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private OrderController controller;

  @Test
  void testCreateOrder() throws Exception {
    // Test POST 
    String requestBody = "{\"numbers\":[1,2,3]}";
    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").exists());
  }

  @Test
  void testGetOrderById() throws Exception {
    // Test GET /{id}
    mockMvc.perform(get("/api/orders/{id}", "1")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").exists());
  }

  @Test
  void testListOrders() throws Exception {
    // Test GET  with query parameters
    mockMvc.perform(get("/api/orders")
            .param("status", "TestValue")
            .param("customerId", "TestValue")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").exists());
  }

  @Test
  void testUpdateOrderStatus() throws Exception {
    // Test PUT /{id}/status
    String requestBody = "{\"numbers\":[1,2,3]}";
    mockMvc.perform(put("/api/orders/{id}/status", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").exists());
  }

  @Test
  void testCancelOrder() throws Exception {
    // Test DELETE /{id}
    mockMvc.perform(delete("/api/orders/{id}", "1")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").exists());
  }
}
