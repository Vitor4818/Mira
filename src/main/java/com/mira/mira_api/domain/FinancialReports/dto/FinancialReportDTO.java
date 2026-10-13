package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinancialReportDTO(
        @JsonAlias({"revenues", "revenue"})
        RevenueDTO revenue,

        @JsonAlias({"clients", "client_concentration", "concentration"})
        ClientsDTO clients,

        @JsonAlias({"operationalMetrics", "operational", "operational_metrics"})
        OperationalDTO operational,

        @JsonAlias({"profitability", "margins"})
        ProfitabilityDTO profitability,

        @JsonAlias({"valuation", "debt"})
        ValuationDTO valuation,

        // Lista dinâmica de verticais de indústria
        @JsonAlias({"industryVerticals", "industry_verticals", "verticals"})
        List<IndustryVerticalDTO> industryVerticals,

        // Lista dinâmica de regiões geográficas
        @JsonAlias({"geographicRevenue", "geographic_revenue", "geographic_exposure", "regions"})
        List<GeographicRegionDTO> geographicRevenue,

        @JsonAlias({"verticalExposure", "vertical_exposure", "exposure_by_vertical"})
        String verticalExposure
) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record IndustryVerticalDTO(
                @JsonAlias({"verticalName", "vertical_name", "name", "industry"})
                String verticalName,

                @JsonAlias({"revenue", "amount"})
                BigDecimal revenue,

                @JsonAlias({"percentage", "share", "percent"})
                BigDecimal percentage
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record GeographicRegionDTO(
                @JsonAlias({"regionName", "region_name", "name", "region", "country"})
                String regionName,

                @JsonAlias({"revenue", "amount"})
                BigDecimal revenue,

                @JsonAlias({"percentage", "share", "percent"})
                BigDecimal percentage
        ) {}
}