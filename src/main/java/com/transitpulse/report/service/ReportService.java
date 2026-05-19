package com.transitpulse.report.service;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.auth.exception.CurrentUserNotFoundException;
import com.transitpulse.common.dto.PageResponse;
import com.transitpulse.report.dto.CreateReportRequest;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.entity.ReportConfirmation;
import com.transitpulse.report.entity.ReportStatus;
import com.transitpulse.report.entity.ReportType;
import com.transitpulse.report.event.ReportVerifiedEvent;
import com.transitpulse.report.exception.ModeratorRoleRequiredException;
import com.transitpulse.report.exception.ReportAlreadyConfirmedException;
import com.transitpulse.report.exception.ReportAuthorCannotConfirmException;
import com.transitpulse.report.exception.ReportNotFoundException;
import com.transitpulse.report.exception.ReportNotPendingException;
import com.transitpulse.report.mapper.ReportMapper;
import com.transitpulse.report.repository.ReportConfirmationRepository;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(CurrentUserNotFoundException::new);

        Report report = reportMapper.toEntity(request, author);

        if (isPrivileged(author)) {
            report.verify(Instant.now());
        }

        Report savedReport = reportRepository.save(report);
        publishVerifiedEventIfVerified(savedReport);
        return toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> getAll(Pageable pageable, ReportStatus status, ReportType type) {
        return PageResponse.from(
                reportRepository.findAll(buildSpecification(status, type), pageable)
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long id) {
        return reportRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(ReportNotFoundException::new);
    }

    @Transactional
    public ReportResponse confirm(Long reportId, AuthenticatedUser currentUser) {
        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(ReportNotFoundException::new);
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(CurrentUserNotFoundException::new);

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
                .orElseThrow(ReportNotFoundException::new);

        validatePending(report, "approved");

        if (report.verify(Instant.now())) {
            publishVerifiedEventIfVerified(report);
        }

        return toResponse(report);
    }

    @Transactional
    public ReportResponse reject(Long reportId, AuthenticatedUser currentUser) {
        ensureModeratorOrAdmin(currentUser);

        Report report = reportRepository.findByIdForUpdate(reportId)
                .orElseThrow(ReportNotFoundException::new);

        validatePending(report, "rejected");

        report.reject();

        return toResponse(report);
    }

    private void validateConfirmation(Report report, User user) {
        validatePending(report, "confirmed");

        if (report.getAuthor().getId().equals(user.getId())) {
            throw new ReportAuthorCannotConfirmException();
        }

        if (reportConfirmationRepository.existsByReportIdAndUserId(report.getId(), user.getId())) {
            throw new ReportAlreadyConfirmedException();
        }
    }

    private void validatePending(Report report, String action) {
        if (!report.isPending()) {
            throw new ReportNotPendingException(action);
        }
    }

    private void ensureModeratorOrAdmin(AuthenticatedUser currentUser) {
        if (currentUser.role() != Role.MODERATOR && currentUser.role() != Role.ADMIN) {
            throw new ModeratorRoleRequiredException();
        }
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN;
    }

    private Specification<Report> buildSpecification(ReportStatus status, ReportType type) {
        Specification<Report> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }

        if (type != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("type"), type));
        }

        return specification;
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
