package dev.csyhorizon.chatbe.singleChat.service;

import dev.csyhorizon.chatbe.singleChat.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SingleChatService {

    private final ReactiveRedisOperations<String, String> redisOperations;
    private static final String CHAT_CHANNEL = "chat_channel";
    private static final String CHAT_BUFFER_KEY = "chat_buffer";

    public Mono<Void> sendMessage(ChatRequest chatRequest) {
        String message = chatRequest.getSender() + ":" + chatRequest.getContent();

        return redisOperations.opsForList().rightPush(CHAT_BUFFER_KEY, message)
                .doOnSuccess(v -> System.out.println("Redis List 저장 완료: " + message))
                .then(redisOperations.convertAndSend(CHAT_CHANNEL, message))
                .doOnSuccess(v -> System.out.println("Redis Pub/Sub 전송 완료: " + message))
                .then();
    }

    public Flux<String> getChatStream() {
        return redisOperations.listenToChannel(CHAT_CHANNEL)
                .map(ReactiveSubscription.Message::getMessage);
    }
}
