package com.mira.mira_api.totvs.dto;

import java.math.BigDecimal;

public record GeographicRegionDTO(
        String name,
        BigDecimal revenue,
        BigDecimal percentage
) {}