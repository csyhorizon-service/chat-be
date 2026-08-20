package dev.csyhorizon.chatbe.domain.report.dto;

import lombok.Data;

@Data
public class ReportRequestDto {
    private String reportedUserId;
    private String roomId;
    private String originalCiphertext;
    private String decryptedPlaintext;
    private String digitalSignature;
    private String senderPublicKeyBase64; // 서명 검증용 송신자의 공개키
}
