package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OperationalDTO(
        @JsonPropertyDescription("Taxa de ocupação, utilização, billable utilization ou taxa de alocação de profissionais (%)")
        @JsonAlias({
                "utilizationRate", "utilization_rate", "utilization", "taxa_utilizacao", "utilizationRatePercentage",
                "billableUtilization", "billable_utilization",
                "resourceUtilization", "resource_utilization",
                "capacityUtilization", "capacity_utilization",
                "consultantUtilization", "consultant_utilization",
                "employeeUtilization", "employee_utilization",
                "taxa_alocacao", "indice_alocacao"
        })
        BigDecimal utilizationRate,

        @JsonPropertyDescription("Attrition, turnover ou taxa de rotatividade/saída de funcionários (%)")
        @JsonAlias({
                "attrition", "turnover", "rotatividade", "taxa_rotatividade",
                "voluntaryAttrition", "voluntary_attrition", "attritionRate", "attrition_rate"
        })
        BigDecimal attrition,

        @JsonPropertyDescription("Número total de funcionários, colaboradores ou headcount")
        @JsonAlias({"employees", "headcount", "numero_funcionarios", "total_colaboradores", "totalEmployees"})
        Integer employees,

        @JsonPropertyDescription("Receita média por profissional, funcionário ou consultor")
        @JsonAlias({"revenuePerProfessional", "revenue_per_professional", "receita_por_profissional", "rev_per_employee"})
        BigDecimal revenuePerProfessional
) {}