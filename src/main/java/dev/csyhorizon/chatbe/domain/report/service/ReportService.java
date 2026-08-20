package dev.csyhorizon.chatbe.domain.report.service;

import dev.csyhorizon.chatbe.domain.report.dto.ReportRequestDto;
import dev.csyhorizon.chatbe.domain.report.entity.Report;
import dev.csyhorizon.chatbe.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SignatureVerifier signatureVerifier;
    private final ReportRepository reportRepository;

    public Mono<Report> processReport(String reporterId, ReportRequestDto dto) {
        // 1. 서명 검증 (데이터 위변조 방지)
        boolean isValid = signatureVerifier.verifySignature(
                dto.getSenderPublicKeyBase64(),
                dto.getOriginalCiphertext(),
                dto.getDigitalSignature()
        );

        if (!isValid) {
            return Mono.error(new IllegalArgumentException("Digital signature verification failed. Data might be tampered."));
        }

        // 2. 검증된 데이터만 DB에 영구 보관
        Report report = Report.builder()
                .reporterId(reporterId)
                .reportedUserId(dto.getReportedUserId())
                .roomId(dto.getRoomId())
                .originalCiphertext(dto.getOriginalCiphertext())
                .decryptedPlaintext(dto.getDecryptedPlaintext())
                .digitalSignature(dto.getDigitalSignature())
                .createdAt(LocalDateTime.now())
                .build();

        return reportRepository.save(report);
    }
}
