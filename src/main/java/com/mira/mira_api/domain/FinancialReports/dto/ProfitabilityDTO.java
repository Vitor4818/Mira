package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

public record ProfitabilityDTO(
        @JsonPropertyDescription("Margem Bruta ou Margem Bruta Ajustada (%)")
        BigDecimal grossMargin,

        @JsonPropertyDescription("Margem EBITDA ou % EBITDA Ajustado")
        BigDecimal ebitdaMargin,

        @JsonPropertyDescription("Despesas SG&A (Vendas, Gerais e Administrativas) ou Despesas Operacionais em relação à Receita Líquida (%)")
        BigDecimal sgaToRevenue
) {}