package com.mira.mira_api.infrastructure.external.globant;

import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GlobantReportCollector implements CompanyReportCollectorRepository {

    private static final Logger log = LoggerFactory.getLogger(GlobantReportCollector.class);
    private static final String TARGET_URL = "https://investors.globant.com/quarterly-earnings";
    private static final String TEMP_STORAGE = "temp_reports";

    @Override
    public boolean supports(String providerType) {
        return "GLOBANT".equalsIgnoreCase(providerType) || "GLOB".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        log.info("Scrapeando a página de relatórios da Globant para o ano {}: {}", year, TARGET_URL);

        try {
            Document doc = Jsoup.connect(TARGET_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(20000)
                    .get();

            Elements links = doc.select("a[href]");
            List<DocumentMeta> documentMetas = new ArrayList<>();
            Set<String> processedUrls = new HashSet<>();

            for (Element link : links) {
                String href = link.attr("abs:href");
                String title = link.text().trim();
                String titleLower = title.toLowerCase();
                String hrefLower = href.toLowerCase();

                if (href.isEmpty() || processedUrls.contains(href)) {
                    continue;
                }

                // Filtra apenas links que contêm o ano especificado no título ou no URL
                if (!hrefLower.contains(year) && !titleLower.contains(year)) {
                    continue;
                }

                // Identifica o tipo do documento e sua relevância
                String category = categorizeDocument(titleLower, hrefLower);
                if (category == null) {
                    continue;
                }

                int quarter = extractQuarter(title + " " + href);
                processedUrls.add(href);

                Path targetDir = Paths.get(TEMP_STORAGE);
                Files.createDirectories(targetDir);

                String localFilePath;
                String fileUrl = href;

                // 1. CASO SEJA PDF NATIVO (Apresentações, Infográficos, 6-K, etc.)
                if (hrefLower.endsWith(".pdf") || hrefLower.contains(".pdf")) {
                    String fileName = String.format("GLOB_Q%dT%s_%s.pdf", quarter, year, sanitizeFileName(category));
                    File pdfFile = targetDir.resolve(fileName).toFile();

                    log.info("Baixando PDF nativo da Globant ({}) de: {}", category, href);
                    downloadFile(href, pdfFile);

                    localFilePath = pdfFile.toURI().toString();
                }
                // 2. CASO SEJA PÁGINA HTML DE PRESS RELEASE (Gera PDF sintético)
                else if (titleLower.contains("press release") || titleLower.contains("results release")) {
                    String fileName = String.format("GLOB_Q%dT%s_Earnings_Release.pdf", quarter, year);
                    String pageText = fetchPressReleasePlainText(href);
                    File generatedPdf = createPdfFromReleaseText(fileName, title, pageText);

                    localFilePath = generatedPdf.toURI().toString();
                } else {
                    continue;
                }

                String docTitle = String.format("Globant Q%dT%s %s", quarter, year, formatCategoryName(category));
                String metaId = UUID.nameUUIDFromBytes((docTitle + quarter + year).getBytes()).toString();

                log.info("Relatório mapeado da Globant: [{}] -> {}", docTitle, localFilePath);

                documentMetas.add(new DocumentMeta(
                        metaId,
                        fileUrl,
                        docTitle,
                        quarter,
                        Integer.parseInt(year),
                        category,
                        localFilePath,
                        null
                ));
            }

            log.info("Total de relatórios da Globant encontrados para o ano {}: {}", year, documentMetas.size());
            return documentMetas;

        } catch (Exception e) {
            log.error("Erro ao coletar relatórios da Globant: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String categorizeDocument(String titleLower, String hrefLower) {
        if (titleLower.contains("presentation") || hrefLower.contains("presentation") || titleLower.contains("deck")) {
            return "earnings_presentation";
        }
        if (titleLower.contains("infographic") || hrefLower.contains("infographic") || titleLower.contains("factsheet")) {
            return "infographic";
        }
        if (titleLower.contains("6-k") || hrefLower.contains("6-k") || titleLower.contains("financial statement")) {
            return "financial_statement";
        }
        if (titleLower.contains("press release") || titleLower.contains("earnings release") || hrefLower.contains("release")) {
            return "earnings_release";
        }
        return null;
    }

    private String formatCategoryName(String category) {
        return switch (category) {
            case "earnings_presentation" -> "Earnings Presentation";
            case "infographic" -> "Infographic";
            case "financial_statement" -> "Financial Statement";
            default -> "Earnings Release";
        };
    }

    private void downloadFile(String urlStr, File destination) throws Exception {
        URI uri = new URI(urlStr);
        try (InputStream in = uri.toURL().openStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String fetchPressReleasePlainText(String pressReleaseUrl) {
        try {
            Document pressDoc = Jsoup.connect(pressReleaseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

            Element bodyContent = pressDoc.selectFirst(".field-item, .node__content, article, main, .press-release-content");
            return bodyContent != null ? bodyContent.text() : pressDoc.text();
        } catch (Exception e) {
            log.warn("Não foi possível extrair o texto de {}: {}", pressReleaseUrl, e.getMessage());
            return "";
        }
    }

    private File createPdfFromReleaseText(String fileName, String title, String content) throws Exception {
        Path targetDir = Paths.get(TEMP_STORAGE);
        Files.createDirectories(targetDir);

        File pdfFile = targetDir.resolve(fileName).toFile();

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream stream = new PDPageContentStream(doc, page);
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            stream.newLineAtOffset(40, 760);
            stream.showText("GLOBANT S.A. - " + sanitizeText(title));
            stream.endText();

            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
            stream.newLineAtOffset(40, 740);

            String[] words = content.split("\\s+");
            StringBuilder line = new StringBuilder();
            int yOffset = 740;

            for (String word : words) {
                String sanitizedWord = sanitizeText(word);
                if (line.length() + sanitizedWord.length() > 110) {
                    stream.showText(line.toString());
                    stream.newLineAtOffset(0, -10);
                    yOffset -= 10;
                    line = new StringBuilder();

                    if (yOffset < 40) {
                        stream.endText();
                        stream.close();

                        page = new PDPage();
                        doc.addPage(page);
                        stream = new PDPageContentStream(doc, page);
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                        stream.newLineAtOffset(40, 760);
                        yOffset = 760;
                    }
                }
                line.append(sanitizedWord).append(" ");
            }

            if (line.length() > 0) {
                stream.showText(line.toString());
            }

            stream.endText();
            stream.close();

            doc.save(pdfFile);
        }
        return pdfFile;
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\\x20-\\x7E]", " ");
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private int extractQuarter(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("q1") || lower.contains("first quarter") || lower.contains("1st quarter")) return 1;
        if (lower.contains("q2") || lower.contains("second quarter") || lower.contains("2nd quarter")) return 2;
        if (lower.contains("q3") || lower.contains("third quarter") || lower.contains("3rd quarter")) return 3;
        if (lower.contains("q4") || lower.contains("fourth quarter") || lower.contains("4th quarter") || lower.contains("full year")) return 4;

        Pattern pattern = Pattern.compile("(?i)q([1-4])");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }
}