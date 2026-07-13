package dev.csyhorizon.chatbe.singleChat.controller;

import dev.csyhorizon.chatbe.singleChat.model.ChatRequest;
import dev.csyhorizon.chatbe.singleChat.service.SingleChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/singlechat")
@RequiredArgsConstructor
public class SingleChatController {

    // service
    private final SingleChatService singleChatService;

    @PostMapping("/send")
    public Mono<Void> send(
            @RequestBody ChatRequest chatRequest
    ) {
        return singleChatService.sendMessage(chatRequest);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream() {
        return singleChatService.getChatStream();
    }
}
