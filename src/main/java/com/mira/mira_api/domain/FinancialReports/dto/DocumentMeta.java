package com.mira.mira_api.domain.FinancialReports.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentMeta(
        @JsonProperty("id") String id,
        @JsonProperty("download_link_id") String downloadLinkId,
        @JsonProperty("file_title") String fileTitle,
        @JsonProperty("file_quarter") Integer fileQuarter,
        @JsonProperty("file_year") Integer fileYear,
        @JsonProperty("internal_name") String internalName,
        @JsonProperty("file_url") String fileUrl,
        @JsonProperty("file_published_date") String filePublishedDate
) {
}