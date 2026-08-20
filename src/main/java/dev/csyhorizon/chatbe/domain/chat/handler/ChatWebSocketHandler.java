package dev.csyhorizon.chatbe.domain.chat.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.csyhorizon.chatbe.domain.chat.dto.ChatMessageDto;
import dev.csyhorizon.chatbe.domain.chat.service.ChatService;
import dev.csyhorizon.chatbe.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 1. 토큰 및 방 번호 추출
        String token = extractToken(session);
        String roomId = extractRoomId(session);

        if (token == null || !jwtTokenProvider.validateToken(token) || roomId == null) {
            log.warn("Invalid token or missing roomId. Closing session: {}", session.getId());
            return session.close();
        }

        String userId = jwtTokenProvider.getUserId(token);
        log.info("User {} connected to room {} with session {}", userId, roomId, session.getId());

        // 2. 클라이언트 -> 서버 (메시지 수신 및 Redis 저장/발행)
        Mono<Void> receive = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .map(this::parseMessage)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(msg -> {
                    // 수신된 메시지에 메타데이터 채우기 (조작 방지)
                    msg.setSenderId(userId);
                    msg.setRoomId(roomId);
                    msg.setTimestamp(Instant.now().toEpochMilli());
                    
                    // Redis에 저장(TTL) 후 Pub/Sub 채널로 발행
                    return chatService.saveMessage(msg)
                            .then(chatService.publishMessage(msg));
                })
                .doOnComplete(() -> log.info("Session closed for user {}", userId))
                .doOnError(e -> log.error("Error in receive for user {}", userId, e))
                .then();

        // 3. 서버(Redis Pub/Sub) -> 클라이언트 (메시지 구독 및 전송)
        Flux<WebSocketMessage> sendStream = chatService.subscribeToRoom(roomId)
                .map(msg -> {
                    try {
                        return session.textMessage(objectMapper.writeValueAsString(msg));
                    } catch (JsonProcessingException e) {
                        log.error("JSON Error", e);
                        return session.textMessage("{}"); // 빈 객체 리턴
                    }
                });

        Mono<Void> send = session.send(sendStream)
                .doOnError(e -> log.error("Error in send for user {}", userId, e));

        // 송/수신 스트림 결합하여 세션 유지 (어느 한쪽이 종료되면 세션 종료)
        return Mono.zip(receive, send).then();
    }

    private Optional<ChatMessageDto> parseMessage(String payload) {
        try {
            return Optional.of(objectMapper.readValue(payload, ChatMessageDto.class));
        } catch (JsonProcessingException e) {
            log.warn("Invalid message format: {}", payload);
            return Optional.empty();
        }
    }

    private String extractToken(WebSocketSession session) {
        List<String> protocols = session.getHandshakeInfo().getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            for (String protocol : protocols) {
                String[] parts = protocol.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("eyJ")) return part;
                }
            }
        }
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) return param.substring(6);
            }
        }
        return null;
    }

    private String extractRoomId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("roomId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("roomId=")) return param.substring(7);
            }
        }
        return null; // 방 번호가 없으면 연결 거부
    }
}
