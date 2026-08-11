package com.mira.mira_api.totvs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record totvsResponse(
        boolean success,
        totvsData data
) {
}