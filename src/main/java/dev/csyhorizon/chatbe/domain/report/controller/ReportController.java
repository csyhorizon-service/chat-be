package dev.csyhorizon.chatbe.domain.report.controller;

import dev.csyhorizon.chatbe.domain.report.dto.ReportRequestDto;
import dev.csyhorizon.chatbe.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public Mono<ResponseEntity<String>> submitReport(
            Mono<Principal> principalMono, 
            @RequestBody ReportRequestDto reportRequest) {
            
        // Spring Security의 Principal에서 인증된 사용자 ID 추출
        return principalMono
                .map(Principal::getName)
                .flatMap(reporterId -> reportService.processReport(reporterId, reportRequest))
                .map(report -> ResponseEntity.ok("Report successfully submitted and verified."))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.badRequest().body("Report failed: " + e.getMessage())
                ));
    }
}
