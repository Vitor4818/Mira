package com.mira.mira_api.infrastructure.persistence.jpa.Company;

import com.mira.mira_api.domain.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataCompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByTicker(String ticker);
    Optional<Company> findByNameIgnoreCase(String name);
}