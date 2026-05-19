package com.transitpulse.report.controller;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.common.dto.PageResponse;
import com.transitpulse.report.dto.CreateReportRequest;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.entity.ReportStatus;
import com.transitpulse.report.entity.ReportType;
import com.transitpulse.report.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ReportResponse create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateReportRequest request
    ) {
        return reportService.create(currentUser, request);
    }

    @GetMapping
    public PageResponse<ReportResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type
    ) {
        return reportService.getAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                status,
                type
        );
    }

    @GetMapping("/{id}")
    public ReportResponse getById(@PathVariable Long id) {
        return reportService.getById(id);
    }

    @PostMapping("/{id}/confirm")
    public ReportResponse confirm(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return reportService.confirm(id, currentUser);
    }
}
