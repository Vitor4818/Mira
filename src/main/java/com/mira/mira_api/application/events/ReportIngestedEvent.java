package com.mira.mira_api.application.events;

import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;

public record ReportIngestedEvent(FinancialReport report) {}