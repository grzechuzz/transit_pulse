package com.transitpulse.report.repository;

import com.transitpulse.report.entity.ReportConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportConfirmationRepository extends JpaRepository<ReportConfirmation, Long> {

    boolean existsByReportIdAndUserId(Long reportId, Long userId);

    long countByReportId(Long reportId);
}
