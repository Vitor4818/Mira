package com.mira.mira_api.domain.company.repository;

import com.mira.mira_api.domain.company.entity.Company;

import java.util.Optional;

public interface CompanyRepository {
    Optional<Company>findByTicker(String ticker);
    Optional<Company>findByName(String name);
}
