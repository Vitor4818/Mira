package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

public record ClientsDTO(
        @JsonPropertyDescription("Concentração de receita no maior cliente individual (Top 1) (%). Sinónimos: maior cliente, cliente principal, top 1")
        @JsonAlias({"topClientConcentration", "top_1", "maior_cliente", "top1"})
        BigDecimal topClientConcentration,

        @JsonPropertyDescription("Concentração de receita nos 5 maiores clientes (Top 5) (%). Sinónimos: top 5 clientes, principais clientes")
        @JsonAlias({"top5ClientsConcentration", "top_5", "top5", "top_5_clientes"})
        BigDecimal top5ClientsConcentration,

        @JsonPropertyDescription("Concentração de receita nos 10 maiores clientes (Top 10) (%)")
        @JsonAlias({"top10ClientsConcentration", "top_10", "top10", "top_10_clientes"})
        BigDecimal top10ClientsConcentration,

        @JsonPropertyDescription("Net Retention Rate (NRR) ou Taxa de Retenção Líquida de Receita (%). Sinónimos: nrr, net_retention_rate, retention")
        @JsonAlias({"netRetentionRate", "nrr", "taxa_retencao_liquida"})
        BigDecimal netRetentionRate
) {}