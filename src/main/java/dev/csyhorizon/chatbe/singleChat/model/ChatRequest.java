package dev.csyhorizon.chatbe.singleChat.model;

import lombok.Getter;

public record ChatRequest (
        @Getter
        String sender,
        @Getter
        String content
) {}
