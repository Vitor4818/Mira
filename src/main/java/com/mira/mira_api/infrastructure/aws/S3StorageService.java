package com.mira.mira_api.infrastructure.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadFile(File file, String s3Key) {
        try {
            log.info("☁️ A enviar ficheiro [{}] para o S3 (Bucket: {} / Key: {})...", file.getName(), bucketName, s3Key);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

            String s3Uri = String.format("s3://%s/%s", bucketName, s3Key);
            log.info("✅ Upload para o S3 concluído com sucesso: {}", s3Uri);

            // Remove o ficheiro temporário local após o upload bem-sucedido
            if (file.exists()) {
                file.delete();
            }

            return s3Uri;
        } catch (Exception e) {
            log.error("❌ Erro ao enviar ficheiro para o S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no upload para o S3", e);
        }
    }
}