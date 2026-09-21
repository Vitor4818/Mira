package com.mira.mira_api.domain.FinancialReports.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "revenues")
@Getter
@Setter
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal revenue;

    private BigDecimal revenueGrowth;

    @OneToMany(mappedBy = "revenueEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndustryVertical> industryVerticals = new ArrayList<>();

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "financial_report_id")
    private FinancialReport financialReport;
}