package com.mira.mira_api.domain.FinancialReports.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "geographic_regions")
@Getter
@Setter
public class GeographicRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal revenue;

    private BigDecimal percentage;

    @ManyToOne
    @JoinColumn(name = "revenue_id")
    private Revenue revenueData;

    // getters e setters
}


