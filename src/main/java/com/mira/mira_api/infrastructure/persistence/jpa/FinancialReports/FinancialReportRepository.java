package com.mira.mira_api.infrastructure.persistence.jpa.FinancialReports;

import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {

    // Método utilitário opcional para buscar relatórios já cadastrados por empresa e período
    Optional<FinancialReport> findByCompanyAndYearAndQuarter(String company, Integer year, Integer quarter);
    boolean existsByCompanyAndYearAndQuarter(String company, Integer year, Integer quarter);

    boolean existsByTickerAndYearAndQuarter(String ticker, int year, int quarter);
}