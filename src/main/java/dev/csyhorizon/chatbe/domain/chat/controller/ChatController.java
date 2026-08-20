package dev.csyhorizon.chatbe.domain.chat.controller;

import dev.csyhorizon.chatbe.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ReactiveRedisTemplate<String, ChatMessageDto> redisTemplate;
    private static final String CHAT_HISTORY_PREFIX = "chat_history:";

    // 최근 1일치(TTL 유지되는 동안) 채팅 내역 조회
    @GetMapping("/history/{roomId}")
    public Flux<ChatMessageDto> getChatHistory(@PathVariable String roomId) {
        String key = CHAT_HISTORY_PREFIX + roomId;
        // 최신 메시지부터 보려면 reverseRange를 사용할 수 있음
        // 지금은 오래된 것부터 순서대로 전달
        return redisTemplate.opsForList().range(key, 0, -1);
    }
}
