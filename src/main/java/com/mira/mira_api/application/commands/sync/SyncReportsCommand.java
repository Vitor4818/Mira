package com.mira.mira_api.application.commands.sync;

public record SyncReportsCommand(
        String ticker,
        String year
) {
    public SyncReportsCommand {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker não pode ser nulo ou vazio");
        }
        if (year == null || year.isBlank()) {
            throw new IllegalArgumentException("Ano não pode ser nulo ou vazio");
        }
    }
}