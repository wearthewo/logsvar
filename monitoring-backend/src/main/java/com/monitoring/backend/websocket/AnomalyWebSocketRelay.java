package com.monitoring.backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnomalyWebSocketRelay implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(AnomalyWebSocketRelay.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AnomalyWebSocketRelay(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String anomalyJson = new String(message.getBody());
        log.info("Relaying anomaly to WebSocket: {}", anomalyJson);
        messagingTemplate.convertAndSend("/topic/anomalies", anomalyJson);
    }
}
