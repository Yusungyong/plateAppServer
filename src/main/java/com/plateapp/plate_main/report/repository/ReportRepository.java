package com.plateapp.plate_main.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.plateapp.plate_main.report.entity.Fp40Report;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Fp40Report, Integer> {
    Page<Fp40Report> findByReporterUsername(String reporterUsername, Pageable pageable);

    @Query("""
        select distinct r.targetUsername
        from Fp40Report r
        where r.reporterUsername = :username
          and r.targetUsername is not null
    """)
    List<String> findReportedUsernames(@Param("username") String username);
}
