package com.mira.mira_api.infrastructure.persistence.jpa.Company;

import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.domain.company.repository.CompanyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CompanyRepositoryImpl implements CompanyRepository {

    private final SpringDataCompanyRepository companyRepository;

    public CompanyRepositoryImpl(SpringDataCompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public Optional<Company> findByTicker(String ticker) {
        return companyRepository.findByTicker(ticker);
    }

    @Override
    public Optional<Company> findByName(String name) {
        return companyRepository.findByNameIgnoreCase(name);
    }
}
