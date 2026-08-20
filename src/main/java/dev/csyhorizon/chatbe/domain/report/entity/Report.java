package dev.csyhorizon.chatbe.domain.report.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_reports")
public class Report {
    
    @Id
    private Long id;
    
    private String reporterId; // 신고하는 사람 (수신자)
    private String reportedUserId; // 신고 당하는 사람 (송신자)
    private String roomId;
    
    private String originalCiphertext; // 원래의 암호문
    private String decryptedPlaintext; // 서버에 제공된 평문 (증거 자료)
    private String digitalSignature; // 위변조 방지용 검증 서명
    
    private LocalDateTime createdAt;
}
