package com.transitpulse.report.controller;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/moderation/reports")
public class ReportModerationController {

    private final ReportService reportService;

    @PostMapping("/{id}/approve")
    public ReportResponse approve(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return reportService.approve(id, currentUser);
    }

    @PostMapping("/{id}/reject")
    public ReportResponse reject(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return reportService.reject(id, currentUser);
    }
}
