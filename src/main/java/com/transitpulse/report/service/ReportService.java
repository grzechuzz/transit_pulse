package com.transitpulse.report.service;

import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.report.dto.CreateReportRequest;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.entity.Report;
import com.transitpulse.report.entity.ReportStatus;
import com.transitpulse.report.mapper.ReportMapper;
import com.transitpulse.report.repository.ReportRepository;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportMapper reportMapper;

    @Transactional
    public ReportResponse create(AuthenticatedUser currentUser, CreateReportRequest request) {
        User author = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user not found"));

        Report report = reportMapper.toEntity(request, author);

        if (isPrivileged(author)) {
            report.setStatus(ReportStatus.VERIFIED);
            report.setVerifiedAt(Instant.now());
        }

        return reportMapper.toResponse(reportRepository.save(report));
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

    private boolean isPrivileged(User user) {
        return user.getRole() == Role.MODERATOR || user.getRole() == Role.ADMIN;
    }
}
