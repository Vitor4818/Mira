package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeographicRegionDTO(
        @JsonAlias({"name", "region", "regionName", "region_name", "regiao", "nome"})
        String name,

        @JsonAlias({"revenue", "amount", "value", "receita", "valor"})
        BigDecimal revenue,

        @JsonAlias({"percentage", "share", "weight", "percentagem", "participacao"})
        BigDecimal percentage
) {}