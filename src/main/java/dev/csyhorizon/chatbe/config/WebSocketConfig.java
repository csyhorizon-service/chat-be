package dev.csyhorizon.chatbe.config;

import dev.csyhorizon.chatbe.domain.chat.handler.ChatWebSocketHandler;
import dev.csyhorizon.chatbe.domain.webrtc.handler.SignalingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final SignalingHandler signalingHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/chat", chatWebSocketHandler);
        map.put("/ws/signaling", signalingHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(map);

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("https://csyhorizon.dev");
        corsConfig.addAllowedOrigin("http://localhost:3000"); 
        corsConfig.addAllowedOrigin("http://localhost:5173"); 
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);
        
        handlerMapping.setCorsConfigurations(Map.of(
                "/ws/chat", corsConfig,
                "/ws/signaling", corsConfig
        ));

        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
