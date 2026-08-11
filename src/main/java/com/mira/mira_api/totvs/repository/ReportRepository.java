package com.mira.mira_api.totvs.repository;

import com.mira.mira_api.totvs.model.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    boolean existsByDownloadLinkId(String downloadLinkId);
}