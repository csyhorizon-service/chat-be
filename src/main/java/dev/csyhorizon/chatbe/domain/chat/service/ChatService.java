package dev.csyhorizon.chatbe.domain.chat.service;

import dev.csyhorizon.chatbe.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ReactiveRedisTemplate<String, ChatMessageDto> redisTemplate;
    private static final String CHAT_HISTORY_PREFIX = "chat_history:";
    private static final String CHAT_TOPIC_PREFIX = "chat_room:";

    // 1. 메시지를 Redis에 저장 (1일 TTL)
    public Mono<Void> saveMessage(ChatMessageDto message) {
        String key = CHAT_HISTORY_PREFIX + message.getRoomId();
        return redisTemplate.opsForList().rightPush(key, message)
                .flatMap(size -> {
                    // 리스트가 처음 생성되었거나 만료를 연장하려면 expire 호출
                    // 방에 메시지가 쌓일 때마다 1일로 갱신 (또는 최초 생성 시 1일로 고정)
                    return redisTemplate.expire(key, Duration.ofDays(1));
                })
                .doOnError(e -> log.error("Failed to save message to Redis", e))
                .then();
    }

    // 2. 메시지를 해당 채팅방 채널로 발행 (Pub/Sub)
    public Mono<Long> publishMessage(ChatMessageDto message) {
        String topic = CHAT_TOPIC_PREFIX + message.getRoomId();
        return redisTemplate.convertAndSend(topic, message);
    }

    // 3. 특정 채팅방 채널을 구독 (수신)
    public Flux<ChatMessageDto> subscribeToRoom(String roomId) {
        String topic = CHAT_TOPIC_PREFIX + roomId;
        return redisTemplate.listenToChannel(topic)
                .map(message -> message.getMessage());
    }
}
