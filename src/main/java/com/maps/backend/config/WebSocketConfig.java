package com.maps.backend.config;

import com.maps.backend.websocket.InternalBusWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean
    public InternalBusWebSocketHandler internalBusWebSocketHandler() {
        return new InternalBusWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(internalBusWebSocketHandler(), "/ws/bus")
                .setAllowedOrigins("*");
    }
}
