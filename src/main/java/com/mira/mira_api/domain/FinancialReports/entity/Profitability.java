package com.mira.mira_api.domain.FinancialReports.entity;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "profitability")
@Getter
@Setter
public class Profitability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal grossMargin;

    private BigDecimal ebitdaMargin;

    private BigDecimal sgaToRevenue;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "financial_report_id")
    private FinancialReport financialReport;

    // getters e setters
}
