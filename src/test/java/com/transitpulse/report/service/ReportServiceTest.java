package com.transitpulse.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transitpulse.auth.exception.CurrentUserNotFoundException;
import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.common.dto.PageResponse;
import com.transitpulse.report.dto.CreateReportRequest;
import com.transitpulse.report.dto.ReportAuthorResponse;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.entity.ReportStatus;
import com.transitpulse.report.entity.ReportType;
import com.transitpulse.report.event.ReportVerifiedEvent;
import com.transitpulse.report.exception.ModeratorRoleRequiredException;
import com.transitpulse.report.exception.ReportAlreadyConfirmedException;
import com.transitpulse.report.exception.ReportAuthorCannotConfirmException;
import com.transitpulse.report.exception.ReportNotPendingException;
import com.transitpulse.report.mapper.ReportMapper;
import com.transitpulse.report.repository.ReportConfirmationRepository;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportConfirmationRepository reportConfirmationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportService reportService;

    @Test
    void userCreatesPendingReportWithoutPublishingEvent() {
        User author = user(1L, Role.USER);
        CreateReportRequest request = request();
        Report report = report(author);

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reportMapper.toEntity(request, author)).thenReturn(report);
        when(reportRepository.save(report)).thenAnswer(invocation -> {
            setId(report, 10L);
            return report;
        });
        stubResponse(report, 0L);

        ReportResponse response = reportService.create(currentUser(author), request);

        assertEquals(ReportStatus.PENDING, report.getStatus());
        assertEquals(0L, response.confirmationCount());
        assertEquals(3, response.requiredConfirmations());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void moderatorCreatesVerifiedReportAndPublishesEvent() {
        User author = user(1L, Role.MODERATOR);
        CreateReportRequest request = request();
        Report report = report(author);

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(reportMapper.toEntity(request, author)).thenReturn(report);
        when(reportRepository.save(report)).thenAnswer(invocation -> {
            setId(report, 10L);
            return report;
        });
        stubResponse(report, 0L);

        reportService.create(currentUser(author), request);

        ArgumentCaptor<ReportVerifiedEvent> eventCaptor = ArgumentCaptor.forClass(ReportVerifiedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(ReportStatus.VERIFIED, report.getStatus());
        assertNotNull(report.getVerifiedAt());
        assertEquals(10L, eventCaptor.getValue().reportId());
        assertEquals(1L, eventCaptor.getValue().authorId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllReturnsPagedReportsWithFilters() {
        User author = user(1L, Role.USER);
        Report report = report(author, 10L);
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(reportRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(report), pageable, 11));
        stubResponse(report, 2L);

        PageResponse<ReportResponse> response = reportService.getAll(
                pageable,
                ReportStatus.PENDING,
                ReportType.DELAY
        );

        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(11L, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(1, response.content().size());
        assertEquals(2L, response.content().getFirst().confirmationCount());
    }

    @Test
    void confirmRejectsReportAuthor() {
        User author = user(1L, Role.USER);
        Report report = report(author, 10L);

        when(reportRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));

        assertThrows(
                ReportAuthorCannotConfirmException.class,
                () -> reportService.confirm(10L, currentUser(author))
        );
    }

    @Test
    void confirmRejectsDuplicateConfirmation() {
        User author = user(1L, Role.USER);
        User confirmer = user(2L, Role.USER);
        Report report = report(author, 10L);

        when(reportRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(2L)).thenReturn(Optional.of(confirmer));
        when(reportConfirmationRepository.existsByReportIdAndUserId(10L, 2L)).thenReturn(true);

        assertThrows(
                ReportAlreadyConfirmedException.class,
                () -> reportService.confirm(10L, currentUser(confirmer))
        );
    }

    @Test
    void thirdConfirmationVerifiesReportAndPublishesEvent() {
        User author = user(1L, Role.USER);
        User confirmer = user(2L, Role.USER);
        Report report = report(author, 10L);

        when(reportRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(report));
        when(userRepository.findById(2L)).thenReturn(Optional.of(confirmer));
        when(reportConfirmationRepository.existsByReportIdAndUserId(10L, 2L)).thenReturn(false);
        stubResponse(report, 3L);

        ReportResponse response = reportService.confirm(10L, currentUser(confirmer));

        assertEquals(ReportStatus.VERIFIED, report.getStatus());
        assertNotNull(report.getVerifiedAt());
        assertEquals(3L, response.confirmationCount());
        verify(eventPublisher).publishEvent(any(ReportVerifiedEvent.class));
    }

    @Test
    void approveRequiresModeratorOrAdmin() {
        User user = user(1L, Role.USER);

        assertThrows(
                ModeratorRoleRequiredException.class,
                () -> reportService.approve(10L, currentUser(user))
        );

        verify(reportRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void rejectChangesPendingReportStatusToRejected() {
        User moderator = user(1L, Role.MODERATOR);
        User author = user(2L, Role.USER);
        Report report = report(author, 10L);

        when(reportRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(report));
        stubResponse(report, 0L);

        reportService.reject(10L, currentUser(moderator));

        assertEquals(ReportStatus.REJECTED, report.getStatus());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void approveRejectsAlreadyVerifiedReport() {
        User moderator = user(1L, Role.MODERATOR);
        User author = user(2L, Role.USER);
        Report report = report(author, 10L);
        report.verify(Instant.now());

        when(reportRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(report));

        assertThrows(
                ReportNotPendingException.class,
                () -> reportService.approve(10L, currentUser(moderator))
        );
    }

    @Test
    void createThrowsWhenCurrentUserNoLongerExists() {
        User currentUser = user(1L, Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                CurrentUserNotFoundException.class,
                () -> reportService.create(currentUser(currentUser), request())
        );
    }

    private void stubResponse(Report report, long confirmationCount) {
        when(reportConfirmationRepository.countByReportId(anyLong())).thenReturn(confirmationCount);
        when(reportMapper.toResponse(report)).thenAnswer(invocation -> response(report, confirmationCount));
    }

    private static CreateReportRequest request() {
        return new CreateReportRequest(ReportType.DELAY, "52", null, "Late tram", null);
    }

    private static Report report(User author) {
        return new Report(author, ReportType.DELAY, "52", null, "Late tram", null);
    }

    private static Report report(User author, Long id) {
        Report report = report(author);
        setId(report, id);
        return report;
    }

    private static User user(Long id, Role role) {
        User user = new User("user" + id + "@example.com", "password-hash", "User " + id, role);
        user.setId(id);
        return user;
    }

    private static AuthenticatedUser currentUser(User user) {
        return AuthenticatedUser.from(user);
    }

    private static ReportResponse response(Report report, long confirmationCount) {
        return new ReportResponse(
                report.getId(),
                new ReportAuthorResponse(report.getAuthor().getId(), report.getAuthor().getDisplayName()),
                report.getType(),
                report.getStatus(),
                report.getLineNumber(),
                report.getStopName(),
                report.getDescription(),
                confirmationCount,
                3,
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getVerifiedAt(),
                report.getExpiresAt()
        );
    }

    private static void setId(Report report, Long id) {
        try {
            Field field = Report.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(report, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
