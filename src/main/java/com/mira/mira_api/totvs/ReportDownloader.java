package com.mira.mira_api.totvs;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class ReportDownloader {

    private final RestClient restClient;

    public ReportDownloader() {
        this.restClient = RestClient.create();
    }

    public String downloadPdf(String fileUrl, String fileName) throws IOException {
        Path outputDir = Paths.get("downloads");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Path targetPath = outputDir.resolve(fileName);

        byte[] pdfBytes = restClient.get()
                .uri(fileUrl)
                .retrieve()
                .body(byte[].class);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IOException("O arquivo baixado veio vazio: " + fileUrl);
        }

        Files.write(targetPath, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return targetPath.toAbsolutePath().toString();
    }
}