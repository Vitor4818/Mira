package com.mira.mira_api.domain.FinancialReports.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "vertical_exposure", columnDefinition = "TEXT")
    private String verticalExposure;

    // Relações OneToMany para as tabelas filhas (substituíram o campo de texto antigo)
    @OneToMany(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialReportVertical> industryVerticals = new ArrayList<>();

    @OneToMany(mappedBy = "financialReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialReportGeography> geographicRevenue = new ArrayList<>();

    // Métodos auxiliares para associar os filhos corretamente
    public void addVertical(FinancialReportVertical vertical) {
        if (this.industryVerticals == null) {
            this.industryVerticals = new ArrayList<>();
        }
        industryVerticals.add(vertical);
        vertical.setFinancialReport(this);
    }

    public void addGeography(FinancialReportGeography geography) {
        if (this.geographicRevenue == null) {
            this.geographicRevenue = new ArrayList<>();
        }
        geographicRevenue.add(geography);
        geography.setFinancialReport(this);
    }
}