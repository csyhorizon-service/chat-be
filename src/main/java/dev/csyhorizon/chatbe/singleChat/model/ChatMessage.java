package dev.csyhorizon.chatbe.singleChat.model;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public record ChatMessage (
        @Id Long id,
        String sender,
        String content,
        LocalDateTime createdAt
) {}