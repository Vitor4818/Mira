package com.mira.mira_api.domain.FinancialReports.dto;

import java.math.BigDecimal;

public record GeographicRegionDTO(
        String name,
        BigDecimal revenue,
        BigDecimal percentage
) {}