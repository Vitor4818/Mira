package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;

public record ValuationDTO(
        @JsonAlias({"freeCashFlow", "fcf", "fluxoDeCaixaLivre"})
        BigDecimal freeCashFlow) {}