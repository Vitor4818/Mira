package com.mira.mira_api.infrastructure.downloader;

import com.mira.mira_api.infrastructure.aws.S3StorageService;
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
    private final S3StorageService s3StorageService;

    public ReportDownloader(RestClient.Builder restClientBuilder, S3StorageService s3StorageService) {
        this.restClient = restClientBuilder.build();
        this.s3StorageService = s3StorageService;
    }

    public String downloadAndStore(String url, String fileName, String ticker, String year) {
        File localFile = downloadPdfToLocal(url, fileName);

        // Define a chave estruturada no S3 (ex: reports/EPAM/2026/EPAM_Q2T2026_Earnings_Presentation.pdf)
        String s3Key = String.format("reports/%s/%s/%s", ticker.toUpperCase(), year, fileName);

        // Envia para o S3 e limpa o ficheiro local temporário
        return s3StorageService.uploadFile(localFile, s3Key);
    }

    public File downloadPdf(String url, String fileName) {
        return downloadPdfToLocal(url, fileName);
    }

    private File downloadPdfToLocal(String url, String fileName) {
        try {
            Path targetDirPath = Paths.get(STORAGE_DIR);
            Files.createDirectories(targetDirPath);

            File destination = targetDirPath.resolve(fileName).toFile();

            // 1. Se o arquivo já existe e tem conteúdo no disco, reutiliza
            if (destination.exists() && destination.length() > 0) {
                log.info("📄 Arquivo já disponível localmente no disco: {}", destination.getAbsolutePath());
                return destination;
            }

            // 2. Se a URL apontar para o protocolo s3://, descarrega diretamente via S3StorageService / AWS SDK
            if (url != null && url.startsWith("s3://")) {
                log.info("☁️ Detetado link S3 [{}]. A baixar via S3StorageService...", url);

                // Remove o prefixo s3:// e separa o bucket do object key (ex: s3://bucket-name/path/to/file.pdf)
                String withoutScheme = url.substring(5);
                int firstSlash = withoutScheme.indexOf('/');

                if (firstSlash == -1) {
                    throw new IllegalArgumentException("URL do S3 inválida (falta o caminho do objeto): " + url);
                }

                String s3Key = withoutScheme.substring(firstSlash + 1);

                // Delega o download para o serviço do S3 para guardar diretamente no destino local
                s3StorageService.downloadFileToPath(s3Key, destination.toPath());

                if (destination.exists() && destination.length() > 0) {
                    log.info("Relatório S3 descarregado localmente em: {}", destination.getAbsolutePath());
                    return destination;
                } else {
                    throw new RuntimeException("O ficheiro descarregado do S3 está vazio ou não foi encontrado.");
                }
            }

            // 3. Se a URL apontar para o protocolo file://, resolve o arquivo diretamente
            if (url != null && (url.startsWith("file:") || url.startsWith("file:/"))) {
                File localFile = new File(URI.create(url));
                if (localFile.exists() && localFile.length() > 0) {
                    log.info("📁 Arquivo carregado via file protocol: {}", localFile.getAbsolutePath());
                    return localFile;
                }
            }

            log.info("Baixando relatório via HTTP de [{}] para [{}]...", url, fileName);

            // 4. Download HTTP tradicional caso não seja S3 nem ficheiro local
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

            log.info("Relatório salvo localmente em: {}", destination.getAbsolutePath());
            return destination;

        } catch (Exception e) {
            log.error("Erro ao baixar/obter relatório de {}: {}", url, e.getMessage());
            throw new RuntimeException("Falha no download do PDF: " + e.getMessage(), e);
        }
    }
}