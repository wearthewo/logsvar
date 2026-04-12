package com.monitoring.backend.config;

import com.monitoring.backend.websocket.AnomalyWebSocketRelay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@ConditionalOnProperty(name = "spring.redis.host", havingValue = "localhost", matchIfMissing = true)
public class RedisConfig {

    @Autowired
    private AnomalyWebSocketRelay anomalyWebSocketRelay;

    @Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(anomalyWebSocketRelay, new PatternTopic("anomalies:live"));
        return container;
    }
}
