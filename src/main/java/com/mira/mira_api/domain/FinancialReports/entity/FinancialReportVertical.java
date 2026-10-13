package com.mira.mira_api.domain.FinancialReports.entity;

import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;
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

    @Column(name = "vertical_name", nullable = false)
    private String verticalName;

    private BigDecimal revenue;

    private BigDecimal percentage;
}