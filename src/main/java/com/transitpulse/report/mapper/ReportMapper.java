package com.transitpulse.report.mapper;

import com.transitpulse.report.dto.CreateReportRequest;
import com.transitpulse.report.dto.ReportResponse;
import com.transitpulse.report.entity.Report;
import com.transitpulse.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "confirmationCount", ignore = true)
    @Mapping(target = "requiredConfirmations", ignore = true)
    ReportResponse toResponse(Report report);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    Report toEntity(CreateReportRequest request, User author);
}
