package com.example.omsv6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplicationTests {

  @Test
  void contextLoads() {
    assertTrue(true);
  }

  @Test
  void applicationContextLoads() {
    assertTrue(true, "Spring Boot application context should load");
  }
}
