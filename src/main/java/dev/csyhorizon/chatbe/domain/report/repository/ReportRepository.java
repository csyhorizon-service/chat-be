package dev.csyhorizon.chatbe.domain.report.repository;

import dev.csyhorizon.chatbe.domain.report.entity.Report;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ReportRepository extends ReactiveCrudRepository<Report, Long> {
}
