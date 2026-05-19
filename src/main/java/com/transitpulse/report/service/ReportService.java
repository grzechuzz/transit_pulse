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
            report.verify(Instant.now());
        }

        Report savedReport = reportRepository.save(report);
        publishVerifiedEventIfVerified(savedReport);
        return toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long id) {
        return reportRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    @Transactional
    public ReportResponse confirm(Long reportId, AuthenticatedUser currentUser) {
        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found"));

        validateConfirmation(report, user);

        reportConfirmationRepository.save(new ReportConfirmation(report, user));

        long confirmationCount = reportConfirmationRepository.countByReportId(report.getId());
        if (confirmationCount >= REQUIRED_CONFIRMATIONS) {
            if (report.verify(Instant.now())) {
                publishVerifiedEventIfVerified(report);
            }
        }

        return toResponse(report);
    }

    @Transactional
    public ReportResponse approve(Long reportId, AuthenticatedUser currentUser) {
        ensureModeratorOrAdmin(currentUser);

        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        validatePending(report, "Only pending reports can be approved");

        if (report.verify(Instant.now())) {
            publishVerifiedEventIfVerified(report);
        }

        return toResponse(report);
    }

    @Transactional
    public ReportResponse reject(Long reportId, AuthenticatedUser currentUser) {
        ensureModeratorOrAdmin(currentUser);

        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        validatePending(report, "Only pending reports can be rejected");

        report.reject();

        return toResponse(report);
    }

    private void validateConfirmation(Report report, User user) {
        validatePending(report, "Only pending reports can be confirmed");

        if (report.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report author cannot confirm own report");
        }

        if (reportConfirmationRepository.existsByReportIdAndUserId(report.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report already confirmed by this user");
        }
    }

    private void validatePending(Report report, String message) {
        if (!report.isPending()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void ensureModeratorOrAdmin(AuthenticatedUser currentUser) {
        if (currentUser.role() != Role.MODERATOR && currentUser.role() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderator or admin role is required");
        }
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN;
    }

    private ReportResponse toResponse(Report report) {
        ReportResponse response = reportMapper.toResponse(report);

        return new ReportResponse(
                response.id(),
                response.author(),
                response.type(),
                response.status(),
                response.lineNumber(),
                response.stopName(),
                response.description(),
                reportConfirmationRepository.countByReportId(report.getId()),
                REQUIRED_CONFIRMATIONS,
                response.createdAt(),
                response.updatedAt(),
                response.verifiedAt(),
                response.expiresAt()
        );
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
