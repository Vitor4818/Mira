package com.mira.mira_api.domain.totvs.service;

import com.mira.mira_api.infrastructure.external.mziq.dto.MziqRequest;
import com.mira.mira_api.infrastructure.external.mziq.dto.MziqResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TotvsClient {

    private final RestClient restClient;

    public TotvsClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // No TotvsClient.java:
    public String getReportsRaw(MziqRequest request) {
        return restClient.post()
                .uri("https://apicatalog.mziq.com/filemanager/company/d3be5d49-62e7-4def-a3e1-ab25ff09f153/filter/categories/year/meta")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .body(request)
                .retrieve()
                .body(String.class); // <--- Retorna a String pura sem passar pelo Jackson
    }

    public MziqResponse getReports(MziqRequest request) {
        return restClient.post()
                .uri("https://apicatalog.mziq.com/filemanager/company/d3be5d49-62e7-4def-a3e1-ab25ff09f153/filter/categories/year/meta")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Origin", "https://ri.totvs.com")
                .header("Referer", "https://ri.totvs.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .body(request)
                .retrieve()
                .body(MziqResponse.class);
    }
}