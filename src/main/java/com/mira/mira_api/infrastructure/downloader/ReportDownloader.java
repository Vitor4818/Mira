package com.mira.mira_api.infrastructure.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class ReportDownloader {

    private static final Logger log = LoggerFactory.getLogger(ReportDownloader.class);
    private static final String DOWNLOAD_DIR = "downloads";

    private final RestClient restClient;

    public ReportDownloader() {
        this.restClient = RestClient.create();
    }

    public String downloadPdf(String fileUrl, String fileName) throws IOException {
        Path outputDir = Paths.get(DOWNLOAD_DIR);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Path targetPath = outputDir.resolve(fileName);

        // Se o arquivo já existe no disco e não está vazio, reaproveita
        if (Files.exists(targetPath) && Files.size(targetPath) > 0) {
            log.info("📂 Arquivo já existe em cache local: {}", targetPath.toAbsolutePath());
            return targetPath.toAbsolutePath().toString();
        }

        log.info("🌐 Baixando relatório de [{}] para [{}]...", fileUrl, fileName);

        byte[] pdfBytes = restClient.get()
                .uri(fileUrl)
                .retrieve()
                .body(byte[].class);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IOException("O arquivo baixado veio vazio: " + fileUrl);
        }

        Files.write(targetPath, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("💾 Download concluído com sucesso: {} ({} bytes)", targetPath.getFileName(), pdfBytes.length);

        return targetPath.toAbsolutePath().toString();
    }
}