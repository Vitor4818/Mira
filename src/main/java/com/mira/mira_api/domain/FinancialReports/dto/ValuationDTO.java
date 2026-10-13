package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ValuationDTO(
        @JsonAlias({"freeCashFlow", "fcf", "fluxoDeCaixaLivre", "free_cash_flow", "fluxo_de_caixa_livre"})
        BigDecimal freeCashFlow,

        @JsonAlias({"cashAndEquivalents", "cash", "caixa", "cash_and_equivalents", "caixa_e_equivalentes"})
        BigDecimal cashAndEquivalents,

        @JsonAlias({"grossDebt", "dividaBruta", "gross_debt", "divida_bruta"})
        BigDecimal grossDebt,

        @JsonAlias({"netDebt", "dividaLiquida", "net_debt", "divida_liquida"})
        BigDecimal netDebt,

        @JsonAlias({"netDebtEbitdaRatio", "dividaLiquidaEbitda", "net_debt_ebitda_ratio", "divida_liquida_ebitda", "net_debt_to_ebitda"})
        BigDecimal netDebtEbitdaRatio
) {}