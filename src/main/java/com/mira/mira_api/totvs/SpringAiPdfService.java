package com.mira.mira_api.totvs;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class SpringAiPdfService {

    /**
     * Carrega um PDF do disco e o converte em uma lista de Documents do Spring AI.
     * Cada Document representa uma página contendo o texto e metadados (como page_number).
     */
    public List<Document> readPdfAsDocuments(String fileName) {
        File file = new File("downloads/" + fileName);

        if (!file.exists()) {
            throw new IllegalArgumentException("Arquivo não encontrado em downloads/: " + fileName);
        }

        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .withPageBottomMargin(0)
                .build();

        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                new FileSystemResource(file),
                config
        );

        return reader.get();
    }
}