package com.mira.mira_api.infrastructure.persistence.jpa.FinancialReports;

import com.mira.mira_api.domain.FinancialReports.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    boolean existsByDownloadLinkId(String downloadLinkId);
}