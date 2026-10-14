package com.mira.mira_api.infrastructure.external.mziq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MziqResponse(
        boolean success,
        MziqData data
) {
}