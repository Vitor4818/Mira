package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IndustryVerticalDTO(
        @JsonAlias({"name", "verticalName", "vertical_name", "segmento", "nome"})
        String name,

        @JsonAlias({"revenue", "amount", "value", "receita", "valor"})
        BigDecimal revenue,

        @JsonAlias({"growth", "yoyGrowth", "growthRate", "crescimento", "taxa_crescimento"})
        BigDecimal growth
) {}