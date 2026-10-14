package com.mira.mira_api.infrastructure.external.scraper;

import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.infrastructure.downloader.ReportDownloader;
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

    private final ReportDownloader reportDownloader;

    public EndavaScraperReportCollector(ReportDownloader reportDownloader) {
        this.reportDownloader = reportDownloader;
    }

    @Override
    public boolean supports(String providerType) {
        return "ENDAVA_SCRAPER".equalsIgnoreCase(providerType)
                || "HTML_SCRAPER".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        log.info("Raspando relatórios da Endava no ano {}: {}", year, company.getApiUrl());

        try {
            Document doc = Jsoup.connect(company.getApiUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .timeout(15000)
                    .get();

            List<DocumentMeta> documentMetas = new ArrayList<>();

            Elements yearBoxes = doc.select("div.box.quarterly-results");

            for (Element box : yearBoxes) {
                Element yearHeader = box.selectFirst("div.header h2");
                if (yearHeader == null || !yearHeader.text().trim().contains(year)) {
                    continue;
                }

                Elements quarterSections = box.select("div.row:has(h3)");
                for (Element section : quarterSections) {
                    Element qHeader = section.selectFirst("h3");
                    String quarterText = qHeader != null ? qHeader.text().trim() : "";
                    int quarter = parseQuarter(quarterText);

                    Elements resultLines = section.select("div.result-line");
                    for (Element line : resultLines) {
                        Element titleElem = line.selectFirst(".link-title");
                        String docTitle = titleElem != null ? titleElem.text().trim() : "";

                        Element pdfLink = line.selectFirst("a[href$=.pdf]");
                        if (pdfLink != null) {
                            String originalUrl = pdfLink.absUrl("href");
                            String finalTitle = String.format("%s - %s (%s)", quarterText, docTitle, year);

                            try {
                                log.info("Processando e enviando Endava para o S3: [{}]", finalTitle);

                                // Gera um nome de ficheiro seguro para o S3
                                String safeTitle = finalTitle.replaceAll("[^a-zA-Z0-9-_]", "_");
                                String fileName = String.format("%s_%dT%d_%s.pdf", company.getTicker(), quarter, Integer.parseInt(year), safeTitle);

                                // Faz o download local e envia para o S3
                                String s3Uri = reportDownloader.downloadAndStore(originalUrl, fileName, company.getTicker(), year);

                                documentMetas.add(new DocumentMeta(
                                        UUID.nameUUIDFromBytes(originalUrl.getBytes()).toString(), // 1. id
                                        originalUrl,                                              // 2. downloadLinkId
                                        finalTitle,                                               // 3. fileTitle
                                        quarter,                                                  // 4. fileQuarter
                                        Integer.parseInt(year),                                   // 5. fileYear
                                        "earnings_release",                                       // 6. internalName
                                        s3Uri,                                                    // 7. fileUrl (Aponta para o S3!)
                                        null                                                      // 8. filePublishedDate
                                ));

                                log.info("Endava guardado no S3 com sucesso: [{}] -> {}", finalTitle, s3Uri);

                            } catch (Exception e) {
                                log.warn("Falha ao descarregar/enviar PDF da Endava para o S3: {}", e.getMessage());
                            }
                        }
                    }
                }
            }

            log.info("Total de documentos processados para o S3 ({}) : {}", company.getTicker(), documentMetas.size());
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
            return 4;
        }
        return 1;
    }
}