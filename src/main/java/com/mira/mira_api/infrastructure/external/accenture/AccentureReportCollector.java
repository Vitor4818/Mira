package com.mira.mira_api.infrastructure.external.accenture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AccentureReportCollector implements CompanyReportCollectorRepository {

    private static final Logger log = LoggerFactory.getLogger(AccentureReportCollector.class);
    private static final String BASE_URL = "https://investor.accenture.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AccentureReportCollector(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean supports(String providerType) {
        return "ACCENTURE".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        // Monta a URL dinâmica com o ano passado no comando
        String endpointUrl = String.format(
                "%s/filings-and-reports/earnings-reports/module-holder/earnings-reports-documents?async=1&year=%s&archived=*&page=1&pageSize=10",
                BASE_URL, year
        );

        log.info("🔍 Consultando API da Accenture para {} com ano={}: {}", company.getTicker(), year, endpointUrl);

        try {
            String rawJson = restClient.get()
                    .uri(endpointUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("⚠️ Resposta vazia da API da Accenture");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray()) {
                log.warn("⚠️ Campo 'data' não é um array no retorno da Accenture");
                return Collections.emptyList();
            }

            List<DocumentMeta> documentMetas = new ArrayList<>();

            for (JsonNode quarterNode : dataArray) {
                String quarterTitle = quarterNode.path("title").asText(""); // ex: "Q3FY26"
                int quarterNumber = extractQuarterNumber(quarterTitle);

                JsonNode linksArray = quarterNode.path("links");
                if (!linksArray.isArray()) continue;

                for (JsonNode linkNode : linksArray) {
                    String customLinkText = linkNode.path("customLinkText").asText("");
                    String docExt = linkNode.path("docExt").asText("");
                    String relativeLink = linkNode.path("link").asText("");

                    // Filtramos estritamente o Press Release contábil em PDF
                    if ("PDF".equalsIgnoreCase(docExt.trim()) && isEarningsRelease(customLinkText, relativeLink)) {
                        String fullPdfUrl = relativeLink.startsWith("http") ? relativeLink : BASE_URL + relativeLink;
                        String docTitle = String.format("%s - %s", quarterTitle, customLinkText);

                        log.info("📄 Documento contábil identificado para Accenture: [{}] -> {}", docTitle, fullPdfUrl);

                        documentMetas.add(new DocumentMeta(
                                UUID.nameUUIDFromBytes(fullPdfUrl.getBytes()).toString(),
                                fullPdfUrl,
                                docTitle,
                                quarterNumber,
                                Integer.parseInt(year),
                                "earnings_release",
                                fullPdfUrl,
                                quarterNode.path("date").asText(null)
                        ));
                    }
                }
            }

            log.info("📑 Encontrados {} documentos contábeis para Accenture no ano {}", documentMetas.size(), year);
            return documentMetas;

        } catch (Exception e) {
            log.error("Erro ao coletar relatórios da Accenture: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private boolean isEarningsRelease(String text, String link) {
        String combined = (text + " " + link).toLowerCase();
        return combined.contains("earnings-release")
                || combined.contains("earnings press release")
                || combined.contains("earnings_release");
    }

    private int extractQuarterNumber(String title) {
        Pattern pattern = Pattern.compile("(?i)Q([1-4])");
        Matcher matcher = pattern.matcher(title);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }
}