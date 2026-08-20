package dev.csyhorizon.chatbe.domain.webrtc.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingHandler implements WebSocketHandler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String SIGNALING_TOPIC_PREFIX = "webrtc_signaling:";

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String roomId = extractRoomId(session);
        if (roomId == null) {
            log.warn("WebRTC connection missing roomId. Closing session.");
            return session.close();
        }

        String topic = SIGNALING_TOPIC_PREFIX + roomId;

        // 1. 수신: 클라이언트로부터 SDP나 ICE Candidate를 받아 Redis Pub/Sub으로 발행
        Mono<Void> receive = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    log.info("Received signaling message for room {}", roomId);
                    return redisTemplate.convertAndSend(topic, payload);
                })
                .doOnError(e -> log.error("Error receiving signaling message", e))
                .then();

        // 2. 송신: Redis 채널을 구독하여 같은 방의 다른 클라이언트가 보낸 시그널링 메시지를 전달
        Flux<WebSocketMessage> sendStream = redisTemplate.listenToChannel(topic)
                .map(message -> session.textMessage(message.getMessage()))
                .doOnError(e -> log.error("Error sending signaling message", e));

        Mono<Void> send = session.send(sendStream);

        return Mono.zip(receive, send).then();
    }

    private String extractRoomId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("roomId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("roomId=")) return param.substring(7);
            }
        }
        return null;
    }
}
