package com.mira.mira_api.infrastructure.external.mziq.dto;

import java.util.List;

public record MziqRequest(
        List<String> categories,
        String language,
        boolean published,
        String year
) {
}