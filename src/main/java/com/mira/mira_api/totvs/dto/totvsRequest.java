package com.mira.mira_api.totvs.dto;

import java.util.List;

public record totvsRequest(
        List<String> categories,
        String language,
        boolean published,
        String year
) {
}