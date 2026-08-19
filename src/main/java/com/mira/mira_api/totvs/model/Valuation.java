package com.mira.mira_api.totvs.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "valuation")
@Getter
@Setter
public class Valuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal freeCashFlow;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "financial_report_id")
    private FinancialReport financialReport;

    // getters e setters
}
