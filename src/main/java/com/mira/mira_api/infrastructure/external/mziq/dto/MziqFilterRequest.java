package com.mira.mira_api.infrastructure.external.mziq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MziqFilterRequest(
        List<String> categories,
        String language,
        Boolean published,
        Object year
) {
    public static MziqFilterRequest forBrazilianCompany(int year) {
        return new MziqFilterRequest(
                List.of(
                        "central_de_resultados_release_de_resultados",
                        "central_de_resultados_demonstracoes_financeiras_itr_dfp",
                        "central_de_resultados_dfpi",
                        "central_de_resultados_apresentacoes"
                ),
                "pt_BR",
                true,
                year
        );
    }

    public static MziqFilterRequest forUsCompany(int year) {
        return new MziqFilterRequest(
                List.of(
                        "quarterly_results_earnings_release",
                        "quarterly_results_6k_quarterly_filing",
                        "quarterly_results_earnings_presentation",
                        "quarterly_results_financial_statement",
                        "quarterly_results_financial_spreadsheet"
                ),
                "en_US",
                true,
                year
        );
    }
}