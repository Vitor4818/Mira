package com.mira.mira_api.totvs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "clients")
@Getter
@Setter
public class Clients {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal topClientsConcentration;

    private BigDecimal netRetentionRate;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "financial_report_id")
    private FinancialReport financialReport;

    // getters e setters
}