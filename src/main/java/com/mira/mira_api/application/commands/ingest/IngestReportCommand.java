package com.mira.mira_api.application.commands.ingest;

public record IngestReportCommand(
        String fileName,
        String company,
        String ticker,
        Integer year,
        Integer quarter
) {
    public IngestReportCommand {
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("Nome do arquivo obrigatório");
        if (ticker == null || ticker.isBlank()) throw new IllegalArgumentException("Ticker obrigatório");
        if (year == null || quarter == null) throw new IllegalArgumentException("Ano e trimestre obrigatórios");
    }
}