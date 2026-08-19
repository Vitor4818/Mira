package com.mira.mira_api.totvs.dto;

public record IngestRequestDTO(
        String fileName,
        String company,
        String ticker,
        Integer year,
        Integer quarter
) {
    public IngestRequestDTO {
        if (company == null) company = "TOTVS";
        if (ticker == null) ticker = "TOTS3";
    }
}