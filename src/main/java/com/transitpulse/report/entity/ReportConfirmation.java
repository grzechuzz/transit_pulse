package com.transitpulse.report.entity;

import com.transitpulse.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "report_confirmations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_confirmations_report_user",
                columnNames = {"report_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ReportConfirmation(Report report, User user) {
        this.report = report;
        this.user = user;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
