package com.mira.mira_api.totvs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record totvsData(
        @JsonProperty("document_metas")
        List<DocumentMeta> documentMetas
) {
}