package com.mira.mira_api.domain.FinancialReports.dto;

public record ProcessReportRequest(
        String fileName,
        String company,
        String ticker,
        Integer year,
        Integer quarter
) {
    // Define valores padrão caso não venham no JSON
    public ProcessReportRequest {
        if (company == null) company = "TOTVS";
        if (ticker == null) ticker = "TOTS3";
    }
}