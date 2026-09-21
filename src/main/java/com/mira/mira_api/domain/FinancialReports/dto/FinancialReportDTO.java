package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinancialReportDTO(
        @JsonAlias({"revenues", "revenue"})
        RevenueDTO revenue,

        ClientsDTO clients,

        @JsonAlias({"operationalMetrics", "operational"})
        OperationalDTO operational,

        ProfitabilityDTO profitability,

        ValuationDTO valuation
) {}