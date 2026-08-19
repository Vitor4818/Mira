package com.mira.mira_api.totvs.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.util.List;

public record RevenueDTO(
        @JsonPropertyDescription("Receita Líquida total das vendas em R$ milhões")
        BigDecimal revenue,

        @JsonPropertyDescription("Crescimento percentual da Receita Líquida em relação ao período anterior (%)")
        BigDecimal revenueGrowth,

        @JsonPropertyDescription("Detalhamento de receita por verticais de indústria ou segmentos (ex: Techfin, Gestão, Varejo, Manufatura)")
        List<IndustryVerticalDTO> industryVerticals,

        @JsonPropertyDescription("Distribuição regional ou geográfica da receita")
        List<GeographicRegionDTO> geographicRegions
) {}