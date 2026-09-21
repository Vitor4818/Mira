package com.mira.mira_api.infrastructure.external.scraper;

import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EndavaScraperReportCollector implements CompanyReportCollectorRepository {

    private static final Logger log = LoggerFactory.getLogger(EndavaScraperReportCollector.class);

    @Override
    public boolean supports(String providerType) {
        return "ENDAVA_SCRAPER".equalsIgnoreCase(providerType)
                || "HTML_SCRAPER".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        log.info("🌐 Raspando relatórios da Endava no ano {}: {}", year, company.getApiUrl());

        try {
            Document doc = Jsoup.connect(company.getApiUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .get();

            List<DocumentMeta> documentMetas = new ArrayList<>();

            // 1. Localiza a caixa do ano correspondente (ex: id="2026-27080-results" ou h2 contendo o ano)
            Elements yearBoxes = doc.select("div.box.quarterly-results");

            for (Element box : yearBoxes) {
                Element yearHeader = box.selectFirst("div.header h2");
                if (yearHeader == null || !yearHeader.text().trim().contains(year)) {
                    continue; // Ignora outros anos
                }

                // 2. Itera pelos trimestres dentro do ano (Q1, Q2, Q3, FY/Q4)
                Elements quarterSections = box.select("div.row:has(h3)");
                for (Element section : quarterSections) {
                    Element qHeader = section.selectFirst("h3");
                    String quarterText = qHeader != null ? qHeader.text().trim() : "";
                    int quarter = parseQuarter(quarterText);

                    // 3. Itera pelas linhas de arquivos do trimestre
                    Elements resultLines = section.select("div.result-line");
                    for (Element line : resultLines) {
                        Element titleElem = line.selectFirst(".link-title");
                        String docTitle = titleElem != null ? titleElem.text().trim() : "";

                        // Foca nos Releases de Resultados contábeis (Earnings Release)
                        Element pdfLink = line.selectFirst("a[href$=.pdf]");
                        if (pdfLink != null) {
                            String pdfUrl = pdfLink.absUrl("href");
                            String finalTitle = String.format("%s - %s (%s)", quarterText, docTitle, year);

                            log.info("📄 PDF identificado: [{}] -> {}", finalTitle, pdfUrl);

                            documentMetas.add(new DocumentMeta(
                                    UUID.nameUUIDFromBytes(pdfUrl.getBytes()).toString(), // 1. id
                                    pdfUrl,                                               // 2. downloadLinkId
                                    finalTitle,                                           // 3. fileTitle
                                    quarter,                                              // 4. fileQuarter
                                    Integer.parseInt(year),                               // 5. fileYear
                                    "earnings_release",                                   // 6. internalName
                                    pdfUrl,                                               // 7. fileUrl
                                    null                                                  // 8. filePublishedDate (ou data se houver)
                            ));

                        }
                    }
                }
            }

            log.info("📑 Total de documentos encontrados para {}: {}", company.getTicker(), documentMetas.size());
            return documentMetas;

        } catch (IOException e) {
            log.error("Erro ao raspar relatórios da Endava: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private int parseQuarter(String text) {
        if (text == null) return 1;
        Pattern pattern = Pattern.compile("(?i)Q([1-4])");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        if (text.toUpperCase().contains("FY")) {
            return 4; // FY geralmente representa o fechamento anual / Q4
        }
        return 1;
    }
}