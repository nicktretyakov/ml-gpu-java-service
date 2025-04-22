package com.mlhybrid.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(
        WebSocketHandler.class
    );

    private final List<WebSocketSession> sessions =
        new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("WebSocket connection established: {}", session.getId());
        sessions.add(session);
        try {
            session.sendMessage(
                new TextMessage("Connected to ML Hybrid System WebSocket")
            );
        } catch (IOException e) {
            logger.error("Error sending welcome message", e);
        }
    }

    @Override
    public void afterConnectionClosed(
        WebSocketSession session,
        CloseStatus status
    ) {
        logger.info(
            "WebSocket connection closed: {}, status: {}",
            session.getId(),
            status
        );
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(
        WebSocketSession session,
        Throwable exception
    ) {
        logger.error(
            "WebSocket transport error: {}",
            exception.getMessage(),
            exception
        );
    }

    @Override
    protected void handleTextMessage(
        WebSocketSession session,
        TextMessage message
    ) {
        // Handle incoming messages if needed
        logger.info(
            "Received message from client {}: {}",
            session.getId(),
            message.getPayload()
        );
    }

    // Send a simple text update to all connected clients
    public void sendUpdate(String message) {
        logger.info(
            "Broadcasting message to {} clients: {}",
            sessions.size(),
            message
        );
        TextMessage textMessage = new TextMessage(message);

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                logger.error(
                    "Error sending message to session {}: {}",
                    session.getId(),
                    e.getMessage(),
                    e
                );
            }
        }
    }

    // Send a structured task update to all connected clients
    public void sendTaskUpdate(String taskId, String status, Object data) {
        Map<String, Object> update = Map.of(
            "taskId",
            taskId,
            "status",
            status,
            "data",
            data,
            "timestamp",
            System.currentTimeMillis()
        );

        try {
            String json = objectMapper.writeValueAsString(update);
            logger.info("Broadcasting task update: {}", json);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    logger.error(
                        "Error sending task update to session {}: {}",
                        session.getId(),
                        e.getMessage(),
                        e
                    );
                }
            }
        } catch (JsonProcessingException e) {
            logger.error(
                "Error serializing task update: {}",
                e.getMessage(),
                e
            );
        }
    }

    /**
     * Broadcast a structured message to all connected WebSocket clients
     *
     * @param message Map containing the message data to broadcast
     */
    public void broadcastMessage(Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            logger.info("Broadcasting message: {}", json);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    logger.error(
                        "Error sending message to session {}: {}",
                        session.getId(),
                        e.getMessage(),
                        e
                    );
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing message: {}", e.getMessage(), e);
        }
    }
}
