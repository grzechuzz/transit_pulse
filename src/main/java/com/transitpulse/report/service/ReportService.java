package com.transitpulse.report.service;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.report.dto.CreateReportRequest;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.entity.ReportConfirmation;
import com.transitpulse.report.entity.ReportStatus;
import com.transitpulse.report.event.ReportVerifiedEvent;
import com.transitpulse.report.mapper.ReportMapper;
import com.transitpulse.report.repository.ReportConfirmationRepository;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int REQUIRED_CONFIRMATIONS = 3;

    private final ReportRepository reportRepository;
    private final ReportConfirmationRepository reportConfirmationRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportResponse create(AuthenticatedUser currentUser, CreateReportRequest request) {
        User author = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found"));

        Report report = reportMapper.toEntity(request, author);

        if (isPrivileged(author)) {
            markAsVerified(report);
        }

        Report savedReport = reportRepository.save(report);
        publishVerifiedEventIfVerified(savedReport);
        return reportMapper.toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(reportMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long id) {
        return reportRepository.findById(id)
                .map(reportMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    @Transactional
    public ReportResponse confirm(Long reportId, AuthenticatedUser currentUser) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found"));

        validateConfirmation(report, user);

        reportConfirmationRepository.save(new ReportConfirmation(report, user));

        long confirmationCount = reportConfirmationRepository.countByReportId(report.getId());
        if (confirmationCount >= REQUIRED_CONFIRMATIONS) {
            markAsVerified(report);
            publishVerifiedEventIfVerified(report);
        }

        return reportMapper.toResponse(report);
    }

    private void validateConfirmation(Report report, User user) {
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending reports can be confirmed");
        }

        if (report.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report author cannot confirm own report");
        }

        if (reportConfirmationRepository.existsByReportIdAndUserId(report.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report already confirmed by this user");
        }
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN;
    }

    private void markAsVerified(Report report) {
        if (report.getStatus() == ReportStatus.VERIFIED) {
            return;
        }

        report.setStatus(ReportStatus.VERIFIED);
        report.setVerifiedAt(Instant.now());
    }

    private void publishVerifiedEventIfVerified(Report report) {
        if (report.getStatus() != ReportStatus.VERIFIED) {
            return;
        }

        eventPublisher.publishEvent(new ReportVerifiedEvent(
                report.getId(),
                report.getAuthor().getId(),
                report.getType(),
                report.getLineNumber(),
                report.getStopName(),
                report.getVerifiedAt()
        ));
    }
}
