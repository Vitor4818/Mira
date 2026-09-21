package com.mira.mira_api.infrastructure.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfImageService {

    private static final Logger log = LoggerFactory.getLogger(PdfImageService.class);
    private static final float DPI = 150f; // Resolução ideal para tabelas e texto legível
    private static final int DEFAULT_MAX_PAGES = 8; // Destaques e DRE costumam estar nas primeiras páginas

    /**
     * Converte até o limite padrão de páginas (otimização de memória e tokens).
     */
    public List<byte[]> convertPdfToImages(String filePath) throws IOException {
        return convertPdfToImages(filePath, DEFAULT_MAX_PAGES);
    }

    /**
     * Converte as páginas de um PDF em uma lista de bytes PNG até um limite máximo.
     * @param maxPages se <= 0, processa o documento inteiro.
     */
    public List<byte[]> convertPdfToImages(String filePath, int maxPages) throws IOException {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            throw new IOException("Arquivo PDF não encontrado no caminho: " + filePath);
        }

        List<byte[]> images = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            int pagesToRender = (maxPages > 0) ? Math.min(totalPages, maxPages) : totalPages;

            log.info("🖼️ Renderizando {} de {} páginas do PDF [{}] para visão multimodal...",
                    pagesToRender, totalPages, pdfFile.getName());

            for (int i = 0; i < pagesToRender; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "PNG", baos);
                images.add(baos.toByteArray());
            }
        }

        log.info("✅ {} páginas convertidas em imagens PNG prontas para o Bedrock.", images.size());
        return images;
    }
}