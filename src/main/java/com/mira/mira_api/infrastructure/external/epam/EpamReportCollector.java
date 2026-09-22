package com.mira.mira_api.infrastructure.external.epam;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.repository.CompanyReportCollectorRepository;
import com.mira.mira_api.domain.company.entity.Company;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EpamReportCollector implements CompanyReportCollectorRepository {

    private static final Logger log = LoggerFactory.getLogger(EpamReportCollector.class);
    private static final String BASE_URL = "https://investors.epam.com";
    private static final String TEMP_STORAGE = "temp_reports";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public EpamReportCollector(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public boolean supports(String providerType) {
        return "EPAM".equalsIgnoreCase(providerType);
    }

    @Override
    public List<DocumentMeta> fetchReportMetas(Company company, String year) {
        String feedUrl = String.format(
                "%s/feed/PressRelease.svc/GetPressReleaseList?LanguageId=1&bodyType=1&pressReleaseDateFilter=3&categoryId=1cb807d2-208f-4bc3-9133-6a9ad45ac3b0&pageSize=-1&pageNumber=0&tagList=financials&includeTags=true&excludeSelection=1&year=%s",
                BASE_URL, year
        );

        log.info("🔍 Consultando feed Q4 da EPAM para o ano {}: {}", year, feedUrl);

        try {
            String rawJson = restClient.get()
                    .uri(feedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Referer", "https://investors.epam.com/")
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("⚠️ Resposta vazia do feed da EPAM");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode listResult = root.path("GetPressReleaseListResult");

            if (!listResult.isArray()) {
                log.warn("⚠️ Campo 'GetPressReleaseListResult' não é um array");
                return Collections.emptyList();
            }

            List<DocumentMeta> documentMetas = new ArrayList<>();

            for (JsonNode item : listResult) {
                String headline = item.path("Headline").asText("");
                String headlineLower = headline.toLowerCase();

                if (!headlineLower.contains("results for") && !headlineLower.contains("reports results")) {
                    continue;
                }

                int quarter = extractQuarter(headline);
                String rawHtmlBody = item.path("Body").asText("");
                String publishedDate = item.path("PressReleaseDate").asText(null);

                // Converte HTML do release para texto limpo e estruturado
                String plainText = Jsoup.parse(rawHtmlBody).text();

                // Gera o PDF com todas as páginas necessárias para preservar as tabelas financeiras
                String fileName = String.format("EPAM_EPAM_Q%dT%s_Earnings_Release.pdf", quarter, year);
                File generatedPdf = createPdfFromReleaseText(fileName, headline, plainText);

                String fileUrl = generatedPdf.toURI().toString();
                String docTitle = String.format("EPAM Q%dT%s Earnings Release", quarter, year);

                log.info("✅ Release sintetizado com sucesso: [{}] -> {}", docTitle, generatedPdf.getAbsolutePath());

                documentMetas.add(new DocumentMeta(
                        UUID.nameUUIDFromBytes(docTitle.getBytes()).toString(),
                        fileUrl,
                        docTitle,
                        quarter,
                        Integer.parseInt(year),
                        "earnings_release",
                        fileUrl,
                        publishedDate
                ));
            }

            log.info("📑 Encontrados e convertidos {} releases da EPAM para o ano {}", documentMetas.size(), year);
            return documentMetas;

        } catch (Exception e) {
            log.error("Erro ao coletar e processar relatórios da EPAM: {}", e.getMessage(), e);
            return Collections.emptyList();
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
            stream.showText("EPAM SYSTEMS - " + sanitizeText(title));
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

                    // Quebra para uma nova página sempre que o limite inferior for atingido
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
        // Remove carateres fora do conjunto WinAnsi exigido pelo PDFBox para as fontes padrão
        return text.replaceAll("[^\\x20-\\x7E]", " ");
    }

    private int extractQuarter(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("first quarter") || lower.contains("1st quarter") || lower.contains("q1")) return 1;
        if (lower.contains("second quarter") || lower.contains("2nd quarter") || lower.contains("q2")) return 2;
        if (lower.contains("third quarter") || lower.contains("3rd quarter") || lower.contains("q3")) return 3;
        if (lower.contains("fourth quarter") || lower.contains("4th quarter") || lower.contains("q4") || lower.contains("full year")) return 4;

        Pattern pattern = Pattern.compile("(?i)q([1-4])");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }
}