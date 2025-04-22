package com.mlhybrid.controller;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SimpleController {

    private static final Logger logger = LoggerFactory.getLogger(
        SimpleController.class
    );

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        logger.info("Status endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "running");
        response.put("service", "ML Hybrid System - Java Server");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/rust-status")
    public Map<String, Object> getRustStatus() {
        logger.info("Rust status endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("connected", true);
        response.put("rust_service", "ML GPU Server");
        response.put("rust_address", "localhost:50051");
        return response;
    }
}
