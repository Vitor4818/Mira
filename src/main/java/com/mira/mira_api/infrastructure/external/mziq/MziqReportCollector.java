package com.mira.mira_api.infrastructure.external.mziq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.infrastructure.downloader.ReportDownloader;
import com.mira.mira_api.infrastructure.external.mziq.dto.MziqFilterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class MziqReportCollector implements CompanyReportCollectorRepository {

    private static final Logger log = LoggerFactory.getLogger(MziqReportCollector.class);

    private final RestClient restClient;
    private final ReportDownloader reportDownloader;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MziqReportCollector(RestClient.Builder restClientBuilder, ReportDownloader reportDownloader) {
        this.restClient = restClientBuilder.build();
        this.reportDownloader = reportDownloader;
    }

    @Override
    public boolean supports(String providerType) {
        return "MZIQ".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        return fetchReportMetas(company, year, null);
    }

    public List<DocumentMeta> fetchReportMetas(Company company, String year, Integer quarter) {
        int yearInt = Integer.parseInt(year);

        MziqFilterRequest requestBody = isBrazilianMarket(company.getTicker())
                ? MziqFilterRequest.forBrazilianCompany(yearInt)
                : MziqFilterRequest.forUsCompany(yearInt);

        log.info("Consultando MZIQ para {} ({}) com ano={} e trimestre={}", company.getName(), company.getTicker(), year, quarter);

        String rawJson = restClient.post()
                .uri(company.getApiUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (rawJson == null || rawJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode arrayNode = null;

            if (root.hasNonNull("data") && root.get("data").hasNonNull("document_metas") && root.get("data").get("document_metas").isArray()) {
                arrayNode = root.get("data").get("document_metas");
            } else if (root.hasNonNull("document_metas") && root.get("document_metas").isArray()) {
                arrayNode = root.get("document_metas");
            } else if (root.hasNonNull("data") && root.get("data").isArray()) {
                arrayNode = root.get("data");
            } else if (root.isArray()) {
                arrayNode = root;
            }

            if (arrayNode != null) {
                List<DocumentMeta> allMetas = new ArrayList<>();
                for (JsonNode item : arrayNode) {
                    DocumentMeta meta = objectMapper.treeToValue(item, DocumentMeta.class);
                    allMetas.add(meta);
                }

                List<DocumentMeta> targetedMetas = new ArrayList<>();

                for (DocumentMeta meta : allMetas) {
                    boolean isTargetYear = meta.fileYear() != null && meta.fileYear() == yearInt;
                    boolean isTargetQuarter = (quarter == null) || (meta.fileQuarter() != null && meta.fileQuarter().equals(quarter));

                    if (isTargetYear && isTargetQuarter) {
                        String originalUrl = meta.fileUrl();
                        String extension = ".pdf";
                        if (originalUrl != null && originalUrl.toLowerCase().contains(".xls")) {
                            extension = ".xlsx";
                        }

                        String safeTitle = meta.fileTitle() != null ? meta.fileTitle().replaceAll("[^a-zA-Z0-9-_]", "_") : "report";
                        String fileName = String.format("%s_%dT%d_%s%s", company.getTicker(), meta.fileQuarter() != null ? meta.fileQuarter() : 1, yearInt, safeTitle, extension);

                        try {
                            log.info("Processando e enviando para o S3: [{}]", meta.fileTitle());
                            // Faz o download local temporário e envia automaticamente para o S3 via ReportDownloader
                            String s3Uri = reportDownloader.downloadAndStore(originalUrl, fileName, company.getTicker(), year);

                            // Reconstrói o DocumentMeta utilizando exatamente os campos do record DocumentMeta
                            DocumentMeta s3Meta = new DocumentMeta(
                                    meta.id() != null ? meta.id() : UUID.nameUUIDFromBytes((meta.fileTitle() + year).getBytes()).toString(),
                                    meta.downloadLinkId(),
                                    meta.fileTitle(),
                                    meta.fileQuarter(),
                                    meta.fileYear(),
                                    meta.internalName(),
                                    s3Uri, // Atualiza o fileUrl para apontar para o S3 (s3://...)
                                    meta.filePublishedDate()
                            );

                            targetedMetas.add(s3Meta);
                            log.info("Documento processado e guardado no S3: [{}] -> {}", meta.fileTitle(), s3Uri);

                        } catch (Exception e) {
                            log.warn("Falha ao descarregar/enviar documento [{}] para o S3: {}", meta.fileTitle(), e.getMessage());
                        }
                    }
                }

                if (!targetedMetas.isEmpty()) {
                    log.info("Total de {} documentos processados e enviados para o S3 em Q{}/{}",
                            targetedMetas.size(), quarter != null ? quarter : "ALL", yearInt);
                    return targetedMetas;
                }

                log.warn("Nenhum documento correspondente encontrado para Q{}/{} em {}.", quarter, yearInt, company.getTicker());
                return Collections.emptyList();
            }

            log.warn("Não foi possível identificar a lista 'document_metas' no JSON da empresa {}", company.getTicker());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Erro ao processar catálogo MZIQ", e);
            throw new RuntimeException("Falha na recolha de relatórios MZIQ", e);
        }
    }

    private boolean isBrazilianMarket(String ticker) {
        if (ticker == null) return false;
        String t = ticker.trim().toUpperCase();
        return t.endsWith("3") || t.endsWith("4") || t.endsWith("11") || t.equalsIgnoreCase("TOTVS");
    }
}