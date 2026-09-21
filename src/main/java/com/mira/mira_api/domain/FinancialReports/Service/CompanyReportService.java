package com.mira.mira_api.domain.FinancialReports.Service;

import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository; // 👈 A interface
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.domain.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyReportService {

    // Recebe a INTERFACE. O Spring coloca o MziqReportCollector aqui dentro sozinho!
    private final List<CompanyReportCollectorRepository> collectors;
    private final CompanyRepository companyRepository;

    public CompanyReportService(List<CompanyReportCollectorRepository> collectors, CompanyRepository companyRepository) {
        this.collectors = collectors;
        this.companyRepository = companyRepository;
    }

    public List<DocumentMeta> findReportsByTicker(String ticker, String year) {
        Company company = companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada para o ticker: " + ticker));

        return findReports(company, year);
    }

    public List<DocumentMeta> findReports(Company company, String year) {
        CompanyReportCollectorRepository collector = collectors.stream()
                .filter(c -> c.supports(company.getProviderType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum coletor registrado para o tipo: " + company.getProviderType()));

        return collector.fetchReportMetas(company, year);
    }
}