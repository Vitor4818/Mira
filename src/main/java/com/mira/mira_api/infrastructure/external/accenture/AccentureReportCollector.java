package com.mira.mira_api.infrastructure.external.accenture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.infrastructure.downloader.ReportDownloader;
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
    private final ReportDownloader reportDownloader;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AccentureReportCollector(RestClient.Builder restClientBuilder, ReportDownloader reportDownloader) {
        this.restClient = restClientBuilder.build();
        this.reportDownloader = reportDownloader;
    }

    @Override
    public boolean supports(String providerType) {
        return "ACCENTURE".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        String endpointUrl = String.format(
                "%s/filings-and-reports/earnings-reports/module-holder/earnings-reports-documents?async=1&year=%s&archived=*&page=1&pageSize=10",
                BASE_URL, year
        );

        log.info("Consultando API da Accenture para {} com ano={}: {}", company.getTicker(), year, endpointUrl);

        try {
            String rawJson = restClient.get()
                    .uri(endpointUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("Resposta vazia da API da Accenture");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray()) {
                log.warn("Campo 'data' não é um array no retorno da Accenture");
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

                    if ("PDF".equalsIgnoreCase(docExt.trim())) {
                        String category = categorizeDocument(customLinkText, relativeLink);

                        if (category != null) {
                            String originalUrl = relativeLink.startsWith("http") ? relativeLink : BASE_URL + relativeLink;
                            String docTitle = String.format("%s - %s", quarterTitle, customLinkText);

                            try {
                                log.info("Processando e enviando Accenture para o S3: [{}]", docTitle);

                                // Gera um nome de ficheiro seguro para o S3
                                String safeTitle = docTitle.replaceAll("[^a-zA-Z0-9-_]", "_");
                                String fileName = String.format("%s_Q%d_%s_%s.pdf", company.getTicker(), quarterNumber, year, safeTitle);

                                // Faz o download local temporário e envia para o S3
                                String s3Uri = reportDownloader.downloadAndStore(originalUrl, fileName, company.getTicker(), year);

                                documentMetas.add(new DocumentMeta(
                                        UUID.nameUUIDFromBytes(originalUrl.getBytes()).toString(),
                                        originalUrl,
                                        docTitle,
                                        quarterNumber,
                                        Integer.parseInt(year),
                                        category,
                                        s3Uri, // Aponta para a URI do S3 (s3://...)
                                        quarterNode.path("date").asText(null)
                                ));

                                log.info("Accenture guardado no S3 com sucesso: [{}] -> {}", docTitle, s3Uri);

                            } catch (Exception e) {
                                log.warn("Falha ao descarregar/enviar PDF da Accenture para o S3 [{}] : {}", docTitle, e.getMessage());
                            }
                        }
                    }
                }
            }

            log.info("Encontrados e processados {} documentos para o S3 da Accenture no ano {}", documentMetas.size(), year);
            return documentMetas;

        } catch (Exception e) {
            log.error("Erro ao coletar relatórios da Accenture: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String categorizeDocument(String text, String link) {
        String combined = (text + " " + link).toLowerCase();

        if (combined.contains("earnings-release")
                || combined.contains("earnings press release")
                || combined.contains("earnings_release")) {
            return "earnings_release";
        }

        if (combined.contains("presentation")
                || combined.contains("slides")
                || combined.contains("investor presentation")
                || combined.contains("webcast")) {
            return "earnings_presentation";
        }

        return null;
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