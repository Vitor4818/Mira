package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfitabilityDTO(
        @JsonPropertyDescription("Margem Bruta ou Margem Bruta Ajustada (%)")
        @JsonAlias({"grossMargin", "gross_margin", "margem_bruta"})
        BigDecimal grossMargin,

        @JsonPropertyDescription("Margem EBITDA ou % EBITDA Ajustado")
        @JsonAlias({"ebitdaMargin", "ebitda_margin", "margem_ebitda", "operationalMargin", "margem_operacional"})
        BigDecimal ebitdaMargin,

        @JsonPropertyDescription("Margem Líquida (%)")
        @JsonAlias({"netMargin", "net_margin", "margem_liquida"})
        BigDecimal netMargin,

        @JsonPropertyDescription("Margem de Fluxo de Caixa Livre (%)")
        @JsonAlias({"fcfMargin", "fcf_margin", "margem_fcf", "freeCashFlowMargin"})
        BigDecimal fcfMargin,

        @JsonPropertyDescription("Score da Regra dos 40 (Rule of 40)")
        @JsonAlias({"ruleOf40Score", "rule_of_40", "ruleOf40", "regra_dos_40"})
        BigDecimal ruleOf40Score,

        @JsonPropertyDescription("Taxa de Conversão de Fluxo de Caixa Livre")
        @JsonAlias({"freeCashFlowConversion", "fcf_conversion", "conversao_fcf", "conversao_fluxo_caixa"})
        BigDecimal freeCashFlowConversion,

        @JsonPropertyDescription("Despesas SG&A (Vendas, Gerais e Administrativas) em relação à Receita Líquida (%)")
        @JsonAlias({"sgaToRevenue", "sga_to_revenue", "sga", "despesas_sga", "sga_sobre_receita"})
        BigDecimal sgaToRevenue
) {}