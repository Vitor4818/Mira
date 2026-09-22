package com.mira.mira_api.infrastructure.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ReportDownloader {

    private static final Logger log = LoggerFactory.getLogger(ReportDownloader.class);
    private static final String STORAGE_DIR = "temp_reports";

    private final RestClient restClient;

    public ReportDownloader(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public File downloadPdf(String url, String fileName) {
        try {
            Path targetDirPath = Paths.get(STORAGE_DIR);
            Files.createDirectories(targetDirPath);

            File destination = targetDirPath.resolve(fileName).toFile();

            // 1. Se o arquivo já existe e tem conteúdo no disco, reutiliza
            if (destination.exists() && destination.length() > 0) {
                log.info("📄 Arquivo já disponível localmente no disco: {}", destination.getAbsolutePath());
                return destination;
            }

            // 2. Se a URL apontar para o protocolo file://, resolve o arquivo diretamente
            if (url != null && (url.startsWith("file:") || url.startsWith("file:/"))) {
                File localFile = new File(URI.create(url));
                if (localFile.exists() && localFile.length() > 0) {
                    log.info("📁 Arquivo carregado via file protocol: {}", localFile.getAbsolutePath());
                    return localFile;
                }
            }

            log.info("🌐 Baixando relatório de [{}] para [{}]...", url, fileName);

            // 3. Download HTTP tradicional caso não seja arquivo local
            byte[] pdfBytes = restClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .body(byte[].class);

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("Arquivo baixado vazio de " + url);
            }

            try (FileOutputStream fos = new FileOutputStream(destination)) {
                fos.write(pdfBytes);
            }

            log.info("💾 Relatório salvo em: {}", destination.getAbsolutePath());
            return destination;

        } catch (Exception e) {
            log.error("Erro ao baixar/obter relatório de {}: {}", url, e.getMessage());
            throw new RuntimeException("Falha no download do PDF: " + e.getMessage(), e);
        }
    }
}