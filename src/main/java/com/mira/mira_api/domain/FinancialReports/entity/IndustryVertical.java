package com.mira.mira_api.domain.FinancialReports.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "industry_verticals")
@Getter
@Setter
public class IndustryVertical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal revenue;

    private BigDecimal growth;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "revenue_id")
    private Revenue revenueEntity;
}