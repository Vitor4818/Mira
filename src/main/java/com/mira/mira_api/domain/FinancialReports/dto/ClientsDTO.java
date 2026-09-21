package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

public record ClientsDTO(
        @JsonPropertyDescription("Concentração de receita nos Top 10 ou Top 20 maiores clientes (%)")
        BigDecimal topClientsConcentration,

        @JsonPropertyDescription("Net Retention Rate (NRR) ou Taxa de Retenção Líquida de Receita (%)")
        BigDecimal netRetentionRate
) {}