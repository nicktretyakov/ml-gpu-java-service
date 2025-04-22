package com.mlhybrid.controller;

import com.mlhybrid.grpc.GrpcClientService;
import com.mlhybrid.model.ComputeRequest;
import com.mlhybrid.model.ComputeResponse;
import com.mlhybrid.websocket.WebSocketHandler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ml")
public class MLController {

    private static final Logger logger = LoggerFactory.getLogger(
        MLController.class
    );

    private final GrpcClientService grpcClientService;
    private final WebSocketHandler webSocketHandler;

    @Autowired
    public MLController(
        GrpcClientService grpcClientService,
        WebSocketHandler webSocketHandler
    ) {
        this.grpcClientService = grpcClientService;
        this.webSocketHandler = webSocketHandler;
    }

    @PostMapping("/compute")
    public ResponseEntity<ComputeResponse> compute(
        @RequestBody ComputeRequest request
    ) {
        // Generate a task ID if not provided
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            request.setTaskId(UUID.randomUUID().toString());
        }

        logger.info("Received compute request: {}", request);

        // Notify clients that computation has started
        webSocketHandler.sendTaskUpdate(
            request.getTaskId(),
            "started",
            request.getData()
        );

        try {
            // Call the Rust gRPC service
            List<Float> data = request.getData();
            String taskType = request.getTaskType() != null
                ? request.getTaskType()
                : "default";

            logger.info(
                "Sending task to GPU service: {}, type: {}, data points: {}",
                request.getTaskId(),
                taskType,
                data.size()
            );

            ComputeResponse response = grpcClientService.compute(
                data,
                request.getTaskId(),
                taskType
            );

            // Notify clients that computation is complete
            webSocketHandler.sendTaskUpdate(
                request.getTaskId(),
                "completed",
                response.getResult()
            );

            logger.info(
                "Task completed: {}, results: {}",
                request.getTaskId(),
                response.getResult().size()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing computation: {}", e.getMessage(), e);

            // Notify clients about the error
            webSocketHandler.sendTaskUpdate(
                request.getTaskId(),
                "error",
                Map.of("error", e.getMessage())
            );

            // Create error response
            ComputeResponse errorResponse = new ComputeResponse();
            errorResponse.setTaskId(request.getTaskId());
            errorResponse.setStatus("error: " + e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("ML Hybrid System is running");
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = Map.of(
            "system",
            "ML Hybrid Java-Rust",
            "version",
            "1.0.0",
            "description",
            "Distributed computing system for machine learning",
            "components",
            Map.of(
                "java",
                "Spring Boot REST API + WebSocket",
                "rust",
                "GPU-accelerated computing service",
                "communication",
                "gRPC"
            )
        );

        return ResponseEntity.ok(info);
    }
}
