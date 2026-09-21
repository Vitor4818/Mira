package com.mira.mira_api.domain.FinancialReports.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "financial_reports")
@Getter
@Setter
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;
    private String ticker;

    private Integer year;
    private Integer quarter;

    @OneToOne(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private Revenue revenue;

    @OneToOne(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private Clients clients;

    @OneToOne(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private Operational operational;

    @OneToOne(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profitability profitability;

    @OneToOne(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private Valuation valuation;
}