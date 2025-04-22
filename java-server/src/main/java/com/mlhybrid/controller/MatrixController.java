package com.mlhybrid.controller;

import com.mlhybrid.grpc.GrpcClientService;
import com.mlhybrid.model.Matrix;
import com.mlhybrid.model.MatrixComputeRequest;
import com.mlhybrid.model.MatrixComputeResponse;
import com.mlhybrid.websocket.WebSocketHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matrix")
public class MatrixController {

    private static final Logger logger = LoggerFactory.getLogger(
        MatrixController.class
    );

    @Autowired
    private GrpcClientService grpcClientService;

    @Autowired
    private WebSocketHandler webSocketHandler;

    /**
     * Endpoint for matrix computation operations
     */
    @PostMapping("/compute")
    public ResponseEntity<?> compute(
        @RequestBody MatrixComputeRequest request
    ) {
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            request.setTaskId("matrix-" + UUID.randomUUID().toString());
        }

        logger.info(
            "Received matrix compute request: operation={}, taskId={}",
            request.getOperation(),
            request.getTaskId()
        );

        // Validate input matrices
        if (request.getMatrixA() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Matrix A is required"));
        }

        // For operations requiring two matrices, validate matrix B
        if (
            requiresMatrixB(request.getOperation()) &&
            request.getMatrixB() == null
        ) {
            return ResponseEntity.badRequest()
                .body(
                    Map.of(
                        "error",
                        "Matrix B is required for " +
                        request.getOperation() +
                        " operation"
                    )
                );
        }

        // Broadcast task started event via WebSocket
        broadcastTaskStarted(request);

        // Process the request
        try {
            // Send the request to the Rust server via gRPC
            MatrixComputeResponse response = grpcClientService.matrixCompute(
                request
            );

            // For successful operations, broadcast the result
            if ("completed".equals(response.getStatus())) {
                broadcastTaskCompleted(response);
                return ResponseEntity.ok(response);
            } else {
                // Handle computation errors
                broadcastTaskFailed(response);
                return ResponseEntity.unprocessableEntity()
                    .body(
                        Map.of(
                            "error",
                            response.getErrorMessage(),
                            "taskId",
                            response.getTaskId(),
                            "status",
                            response.getStatus()
                        )
                    );
            }
        } catch (Exception e) {
            logger.error(
                "Error processing matrix computation: {}",
                e.getMessage(),
                e
            );

            // Broadcast error via WebSocket
            broadcastTaskError(request.getTaskId(), e.getMessage());

            return ResponseEntity.internalServerError()
                .body(
                    Map.of(
                        "error",
                        "Error processing matrix computation: " +
                        e.getMessage(),
                        "taskId",
                        request.getTaskId()
                    )
                );
        }
    }

    /**
     * Check if the operation requires Matrix B
     */
    private boolean requiresMatrixB(String operation) {
        return (
            "multiply".equals(operation) ||
            "add".equals(operation) ||
            "subtract".equals(operation)
        );
    }

    /**
     * Broadcast task started event via WebSocket
     */
    private void broadcastTaskStarted(MatrixComputeRequest request) {
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", request.getTaskId());
        message.put("status", "started");
        message.put("operation", request.getOperation());
        message.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcastMessage(message);
    }

    /**
     * Broadcast task completed event via WebSocket
     */
    private void broadcastTaskCompleted(MatrixComputeResponse response) {
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", response.getTaskId());
        message.put("status", "completed");
        message.put("executionTimeMs", response.getExecutionTimeMs());
        message.put("timestamp", System.currentTimeMillis());

        if (response.getResultMatrix() != null) {
            message.put("rows", response.getResultMatrix().getRows());
            message.put("cols", response.getResultMatrix().getCols());
        }

        webSocketHandler.broadcastMessage(message);
    }

    /**
     * Broadcast task failed event via WebSocket
     */
    private void broadcastTaskFailed(MatrixComputeResponse response) {
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", response.getTaskId());
        message.put("status", "failed");
        message.put("error", response.getErrorMessage());
        message.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcastMessage(message);
    }

    /**
     * Broadcast error via WebSocket
     */
    private void broadcastTaskError(String taskId, String errorMessage) {
        Map<String, Object> message = new HashMap<>();
        message.put("taskId", taskId);
        message.put("status", "error");
        message.put("error", errorMessage);
        message.put("timestamp", System.currentTimeMillis());

        webSocketHandler.broadcastMessage(message);
    }
}
