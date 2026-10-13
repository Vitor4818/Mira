package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueDTO(
        @JsonAlias({"netRevenue", "revenue", "totalRevenue", "receitaLiquida", "net_revenue", "receita_liquida"})
        BigDecimal revenue,

        @JsonAlias({"revenueGrowth", "growth", "yoyGrowth", "subscriptionRevenueGrowth", "crescimentoYoY", "revenue_growth", "crescimento_yoy"})
        BigDecimal revenueGrowth,

        @JsonAlias({"industryVerticals", "industry_verticals", "verticals", "verticais"})
        List<IndustryVerticalDTO> industryVerticals,

        @JsonAlias({"geographicRegions", "geographic_regions", "regions", "regioes"})
        List<GeographicRegionDTO> geographicRegions
) {}