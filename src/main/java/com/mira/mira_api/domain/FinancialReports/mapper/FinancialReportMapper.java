package com.mira.mira_api.domain.FinancialReports.mapper; // ou .mapper se mover

import com.mira.mira_api.domain.FinancialReports.dto.*;
import com.mira.mira_api.domain.FinancialReports.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FinancialReportMapper {

    public FinancialReport toEntity(FinancialReportDTO dto, String company, String ticker, Integer year, Integer quarter) {
        if (dto == null) return null;

        FinancialReport report = new FinancialReport();
        report.setCompany(company);
        report.setTicker(ticker != null ? ticker.toUpperCase() : null);
        report.setYear(year);
        report.setQuarter(quarter);

        // 1. Revenue
        if (dto.revenue() != null) {
            Revenue revenue = toRevenueEntity(dto.revenue());
            revenue.setFinancialReport(report);
            report.setRevenue(revenue);
        }

        // 2. Clients
        if (dto.clients() != null) {
            Clients clients = toClientsEntity(dto.clients());
            clients.setFinancialReport(report);
            report.setClients(clients);
        }

        // 3. Operational
        if (dto.operational() != null) {
            Operational operational = toOperationalEntity(dto.operational());
            operational.setFinancialReport(report);
            report.setOperational(operational);
        }

        // 4. Profitability
        if (dto.profitability() != null) {
            Profitability profitability = toProfitabilityEntity(dto.profitability());
            profitability.setFinancialReport(report);
            report.setProfitability(profitability);
        }

        // 5. Valuation
        if (dto.valuation() != null) {
            Valuation valuation = toValuationEntity(dto.valuation());
            valuation.setFinancialReport(report);
            report.setValuation(valuation);
        }

        return report;
    }

    private Revenue toRevenueEntity(RevenueDTO dto) {
        Revenue revenue = new Revenue();
        revenue.setRevenue(dto.revenue());
        revenue.setRevenueGrowth(dto.revenueGrowth());

        if (dto.industryVerticals() != null && !dto.industryVerticals().isEmpty()) {
            List<IndustryVertical> verticals = dto.industryVerticals().stream().map(vDto -> {
                IndustryVertical vertical = new IndustryVertical();
                vertical.setName(vDto.name());
                vertical.setRevenue(vDto.revenue());
                vertical.setGrowth(vDto.growth());
                vertical.setRevenueEntity(revenue);
                return vertical;
            }).toList();
            revenue.setIndustryVerticals(verticals);
        }

        return revenue;
    }

    private Clients toClientsEntity(ClientsDTO dto) {
        Clients clients = new Clients();
        clients.setTopClientsConcentration(dto.topClientsConcentration());
        clients.setNetRetentionRate(dto.netRetentionRate());
        return clients;
    }

    private Operational toOperationalEntity(OperationalDTO dto) {
        Operational operational = new Operational();
        operational.setUtilizationRate(dto.utilizationRate());
        operational.setAttrition(dto.attrition());
        operational.setEmployees(dto.employees());
        operational.setRevenuePerProfessional(dto.revenuePerProfessional());
        return operational;
    }

    private Profitability toProfitabilityEntity(ProfitabilityDTO dto) {
        Profitability profitability = new Profitability();
        profitability.setGrossMargin(dto.grossMargin());
        profitability.setEbitdaMargin(dto.ebitdaMargin());
        profitability.setSgaToRevenue(dto.sgaToRevenue());
        return profitability;
    }

    private Valuation toValuationEntity(ValuationDTO dto) {
        Valuation valuation = new Valuation();
        valuation.setFreeCashFlow(dto.freeCashFlow());
        return valuation;
    }
}