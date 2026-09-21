package com.mira.mira_api.infrastructure.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SpringAiPdfService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiPdfService.class);

    /**
     * Lê todas as páginas do PDF e retorna uma lista de Documents do Spring AI.
     */
    public List<Document> readPdfAsDocuments(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IllegalArgumentException("Arquivo PDF não encontrado: " + filePath);
        }

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageBottomMargin(0)
                .build();

        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                new FileSystemResource(file),
                config
        );

        List<Document> documents = reader.get();
        log.info("📄 PDF lido com sucesso via Spring AI: {} páginas extraídas de [{}]",
                documents.size(), file.getName());
        return documents;
    }

    /**
     * Concatena o texto de todas as páginas em uma única String para o Bedrock.
     */
    public String extractFullText(String filePath) {
        List<Document> docs = readPdfAsDocuments(filePath);
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n--- NOVA PÁGINA ---\n\n"));
    }
}