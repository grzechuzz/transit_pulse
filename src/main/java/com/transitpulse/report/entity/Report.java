package com.transitpulse.report.entity;

import com.transitpulse.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "line_number")
    @Setter
    private String lineNumber;

    @Column(name = "stop_name")
    @Setter
    private String stopName;

    @Column(nullable = false)
    @Setter
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "verified_at")
    @Setter
    private Instant verifiedAt;

    @Column(name = "expires_at")
    @Setter
    private Instant expiresAt;

    public Report(
            User author,
            ReportType type,
            String lineNumber,
            String stopName,
            String description,
            Instant expiresAt
    ) {
        this.author = author;
        this.type = type;
        this.lineNumber = lineNumber;
        this.stopName = stopName;
        this.description = description;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
