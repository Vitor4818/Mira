package com.mira.mira_api.totvs.model;

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

    private BigDecimal revenue; // Valor monetário (ex: 1308.2)

    private BigDecimal growth; // Percentual de crescimento (ex: 19.3)

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "revenue_id")
    private Revenue revenueEntity; // Relacionamento com a entidade pai Revenue
}