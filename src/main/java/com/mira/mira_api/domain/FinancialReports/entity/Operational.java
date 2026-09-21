package com.mira.mira_api.domain.FinancialReports.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "operational_metrics")
@Getter
@Setter
public class Operational {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal utilizationRate;

    private BigDecimal attrition;

    private Integer employees;

    private BigDecimal revenuePerProfessional;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "financial_report_id")
    private FinancialReport financialReport;

    // getters e setters
}

