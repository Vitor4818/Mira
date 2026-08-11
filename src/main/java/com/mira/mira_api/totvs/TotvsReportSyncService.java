package com.mira.mira_api.totvs;


import com.mira.mira_api.totvs.dto.DocumentMeta;
import com.mira.mira_api.totvs.dto.totvsRequest;
import com.mira.mira_api.totvs.model.ReportEntity;
import com.mira.mira_api.totvs.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TotvsReportSyncService {

    private static final Logger log = LoggerFactory.getLogger(TotvsReportSyncService.class);

    private final TotvsClient client;
    private final ReportRepository repository;
    private final ReportDownloader downloader;

    public TotvsReportSyncService(TotvsClient client, ReportRepository repository, ReportDownloader downloader) {
        this.client = client;
        this.repository = repository;
        this.downloader = downloader;
    }

    public List<ReportEntity> syncReports(String year) {
        var request = new totvsRequest(
                List.of("central_de_resultados_release_de_resultados"),
                "pt_BR",
                true,
                year
        );

        var response = client.getReports(request);

        if (response == null || response.data() == null || response.data().documentMetas() == null) {
            log.warn("Nenhum relatório encontrado para o ano {}", year);
            return List.of();
        }

        List<DocumentMeta> metas = response.data().documentMetas();
        List<ReportEntity> processedReports = new ArrayList<>();

        for (DocumentMeta meta : metas) {
            String linkId = meta.downloadLinkId() != null ? meta.downloadLinkId() : meta.fileUrl();

            // 1. Verifica no PostgreSQL se o documento já foi baixado
            if (repository.existsByDownloadLinkId(linkId)) {
                log.info("Relatório já baixado anteriormente, pulando: {}", meta.fileTitle());
                continue;
            }

            log.info("Novo relatório detectado! Baixando: {}", meta.fileTitle());

            try {
                // Nome do arquivo salvo no disco
                String fileName = String.format("totvs_%s.pdf",
                        meta.fileTitle().replaceAll("[^a-zA-Z0-9.-]", "_"));

                // 2. Faz o download do PDF
                String localPath = downloader.downloadPdf(meta.fileUrl(), fileName);

                // 3. Cadastra o registro no Banco de Dados
                ReportEntity entity = new ReportEntity(
                        "TOTVS",
                        meta.fileTitle(),
                        meta.fileQuarter(),
                        meta.fileYear(),
                        linkId,
                        meta.fileUrl(),
                        localPath
                );

                ReportEntity saved = repository.save(entity);
                processedReports.add(saved);
                log.info("Relatório salvo no banco com ID: {}", saved.getId());

            } catch (Exception e) {
                log.error("Erro ao baixar relatório {}: {}", meta.fileTitle(), e.getMessage(), e);
            }
        }

        return processedReports;
    }
}