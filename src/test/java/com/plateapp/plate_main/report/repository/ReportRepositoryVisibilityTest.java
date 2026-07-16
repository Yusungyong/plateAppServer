package com.plateapp.plate_main.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateapp.plate_main.report.entity.Fp40Report;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReportRepositoryVisibilityTest {

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void findsOnlyCurrentViewersActiveUnwithdrawnVideoReport() {
        Fp40Report report = activeReport("viewer", "video", 81);
        reportRepository.saveAndFlush(report);

        assertThat(reportRepository
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "viewer", "VIDEO", 81, "Y"))
                .isTrue();
        assertThat(reportRepository
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "other-viewer", "video", 81, "Y"))
                .isFalse();

        report.setUnflaggedAt(LocalDateTime.now());
        reportRepository.saveAndFlush(report);
        assertThat(reportRepository
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "viewer", "video", 81, "Y"))
                .isFalse();
    }

    @Test
    void findsActiveAuthorReportByUsernameOrUserId() {
        Fp40Report report = activeReport("viewer", "user", 321);
        report.setTargetUsername("author");
        reportRepository.saveAndFlush(report);

        assertThat(reportRepository.existsActiveUserReport(
                        "viewer", "author", 321, "USER", "Y"))
                .isTrue();

        report.setTargetUsername(null);
        report.setTargetUserId(321);
        report.setTargetId(999);
        reportRepository.saveAndFlush(report);
        assertThat(reportRepository.existsActiveUserReport(
                        "viewer", "author", 321, "user", "Y"))
                .isTrue();

        report.setTargetFlag("N");
        reportRepository.saveAndFlush(report);
        assertThat(reportRepository.existsActiveUserReport(
                        "viewer", "author", 321, "user", "Y"))
                .isFalse();
    }

    private Fp40Report activeReport(String reporter, String targetType, int targetId) {
        Fp40Report report = new Fp40Report();
        report.setReporterUsername(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setTargetFlag("Y");
        report.setSubmittedAt(LocalDateTime.now());
        return report;
    }
}
