package com.mira.mira_api.domain.FinancialReports.repository;

import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;

import java.util.List;

public interface CompanyReportCollectorRepository {
    boolean supports(String providerType);
    List<DocumentMeta> fetchReportMetas(Company company, String year);

}