package com.mira.mira_api.infrastructure.external.mziq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.infrastructure.external.mziq.dto.MziqFilterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class MziqReportCollector implements CompanyReportCollectorRepository {

    private static final Logger log = LoggerFactory.getLogger(MziqReportCollector.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MziqReportCollector(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
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

        log.info("🔍 Consultando MZIQ para {} ({}) com ano={} e trimestre={}", company.getName(), company.getTicker(), year, quarter);

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
                    log.info("📥 [MZIQ bruto] Título: '{}' | InternalName: '{}' | Ano: {} | Trimestre: {}",
                            meta.fileTitle(), meta.internalName(), meta.fileYear(), meta.fileQuarter());
                }

                List<DocumentMeta> targetedMetas = new ArrayList<>();

                for (DocumentMeta meta : allMetas) {
                    boolean isTargetYear = meta.fileYear() != null && meta.fileYear() == yearInt;
                    boolean isTargetQuarter = (quarter == null) || (meta.fileQuarter() != null && meta.fileQuarter().equals(quarter));

                    if (isTargetYear && isTargetQuarter) {
                        // 🔓 MODO TOTALMENTE PERMISSIVO: Traz absolutamente TUDO o que pertencer ao trimestre/ano
                        // Isto vai garantir que a apresentação entra, seja qual for o nome dela!
                        targetedMetas.add(meta);
                        log.info("🎯 Documento incluído sem restrições: [{}] (Internal: {})", meta.fileTitle(), meta.internalName());
                    }
                }

                if (!targetedMetas.isEmpty()) {
                    log.info("📑 Total de {} documentos enviados para processamento em Q{}/{}",
                            targetedMetas.size(), quarter != null ? quarter : "ALL", yearInt);
                    return targetedMetas;
                }

                log.warn("⚠️ Nenhum documento correspondente encontrado para Q{}/{} em {}.", quarter, yearInt, company.getTicker());
                return allMetas;
            }

            log.warn("⚠️ Não foi possível identificar a lista 'document_metas' no JSON da empresa {}", company.getTicker());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Erro ao converter resposta JSON do MZIQ para DocumentMeta", e);
            throw new RuntimeException("Falha na desserialização do catálogo MZIQ", e);
        }
    }

    private boolean isBrazilianMarket(String ticker) {
        if (ticker == null) return false;
        String t = ticker.trim().toUpperCase();
        return t.endsWith("3") || t.endsWith("4") || t.endsWith("11") || t.equalsIgnoreCase("TOTVS");
    }
}