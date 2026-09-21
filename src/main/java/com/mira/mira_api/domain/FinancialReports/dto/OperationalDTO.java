package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

public record OperationalDTO(
        @JsonPropertyDescription("Taxa de ocupação, utilização ou taxa de alocação de profissionais (%)")
        BigDecimal utilizationRate,

        @JsonPropertyDescription("Attrition, turnover ou taxa de rotatividade/saída de funcionários (%)")
        BigDecimal attrition,

        @JsonPropertyDescription("Número total de funcionários, colaboradores ou headcount")
        Integer employees,

        @JsonPropertyDescription("Receita média por profissional, funcionário ou consultor")
        BigDecimal revenuePerProfessional
) {}