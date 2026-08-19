package com.mira.mira_api.totvs.dto;

public record FinancialReportDTO(
        RevenueDTO revenue,
        ClientsDTO clients,
        OperationalDTO operational,
        ProfitabilityDTO profitability,
        ValuationDTO valuation
) {}