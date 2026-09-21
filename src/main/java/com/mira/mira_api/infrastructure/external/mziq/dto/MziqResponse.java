package com.mira.mira_api.infrastructure.external.mziq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mira.mira_api.domain.totvs.dto.totvsData;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MziqResponse(
        boolean success,
        totvsData data
) {
}