package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueDTO(
        @JsonAlias({"netRevenue", "revenue", "totalRevenue", "receitaLiquida"})
        BigDecimal revenue,

        @JsonAlias({"revenueGrowth", "growth", "yoyGrowth", "subscriptionRevenueGrowth", "crescimentoYoY"})
        BigDecimal revenueGrowth,

        List<IndustryVerticalDTO> industryVerticals,
        List<GeographicRegionDTO> geographicRegions
) {}