package dev.csyhorizon.chatbe.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String roomId;
    private String senderId;
    private String encryptedPayload; // E2EE 적용된 암호문 (Base64)
    private String digitalSignature; // 송신자의 전자 서명 (Base64)
    private long timestamp;
}
