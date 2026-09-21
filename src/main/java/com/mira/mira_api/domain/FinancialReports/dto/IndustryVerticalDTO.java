package com.mira.mira_api.domain.FinancialReports.dto;

import java.math.BigDecimal;

public record IndustryVerticalDTO(
        String name,
        BigDecimal revenue,
        BigDecimal growth
) {}