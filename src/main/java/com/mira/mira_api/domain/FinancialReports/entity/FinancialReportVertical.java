package com.mira.mira_api.domain.FinancialReports.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "financial_report_verticals")
@Data
public class FinancialReportVertical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_report_id", nullable = false)
    private FinancialReport financialReport;

    // Associa à receita a que esta vertical pertence (mapeia o 'industryVerticals' em Revenue)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_id")
    private Revenue revenue;

    // Substitui o antigo 'vertical_name' pela chave estrangeira para o catálogo mestre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vertical_id", nullable = false)
    private IndustryVertical industryVertical;

    private BigDecimal revenueValue;

    private BigDecimal percentage;
}