package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.dto.DocumentMeta;
import com.mira.mira_api.totvs.dto.FinancialReportDTO;
import com.mira.mira_api.totvs.dto.totvsRequest;
import com.mira.mira_api.totvs.model.FinancialReport;
import com.mira.mira_api.totvs.model.ReportEntity;
import com.mira.mira_api.totvs.repository.FinancialReportRepository;
import com.mira.mira_api.totvs.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TotvsReportSyncService {

    private static final Logger log = LoggerFactory.getLogger(TotvsReportSyncService.class);

    private final TotvsClient client;
    private final ReportRepository reportRepository;             // Banco dos PDFs baixados
    private final FinancialReportRepository financialRepository; // Banco dos dados financeiros da IA
    private final ReportDownloader downloader;
    private final SpringAiPdfService pdfService;
    private final FinancialDataExtractor extractor;
    private final FinancialReportMapper mapper;
    private final SlackNotificationService slackService; // Injetar no construtor

    public TotvsReportSyncService(
            TotvsClient client,
            ReportRepository reportRepository,
            FinancialReportRepository financialRepository,
            ReportDownloader downloader,
            SpringAiPdfService pdfService,
            FinancialDataExtractor extractor,
            FinancialReportMapper mapper, SlackNotificationService slackService
    ) {
        this.client = client;
        this.reportRepository = reportRepository;
        this.financialRepository = financialRepository;
        this.downloader = downloader;
        this.pdfService = pdfService;
        this.extractor = extractor;
        this.mapper = mapper;
        this.slackService = slackService;
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
            log.warn("Nenhum relatório encontrado na API da TOTVS para o ano {}", year);
            return List.of();
        }

        List<DocumentMeta> metas = response.data().documentMetas();
        List<ReportEntity> processedReports = new ArrayList<>();

        for (DocumentMeta meta : metas) {
            String linkId = meta.downloadLinkId() != null ? meta.downloadLinkId() : meta.fileUrl();

            // 1. CHECAGEM NO BANCO: Se o PDF já foi baixado, não faz nada
            if (reportRepository.existsByDownloadLinkId(linkId)) {
                log.info("Relatório já baixado e sincronizado anteriormente, pulando: {}", meta.fileTitle());
                continue;
            }

            log.info("Novo relatório detectado! Baixando do MZIQ: {}", meta.fileTitle());

            try {
                // Nome limpo do arquivo no disco
                String fileName = String.format("totvs_%s.pdf",
                        meta.fileTitle().replaceAll("[^a-zA-Z0-9.-]", "_"));

                // 2. SEU DOWNLOADER: Baixa o PDF para /downloads
                String localPath = downloader.downloadPdf(meta.fileUrl(), fileName);

                // 3. PERSISTÊNCIA DE METADADOS: Cadastra o relatório baixado
                ReportEntity entity = new ReportEntity(
                        "TOTVS",
                        meta.fileTitle(),
                        meta.fileQuarter(),
                        meta.fileYear(),
                        linkId,
                        meta.fileUrl(),
                        localPath
                );

                ReportEntity savedReport = reportRepository.save(entity);
                processedReports.add(savedReport);
                log.info("Relatório salvo no Postgres com ID: {}", savedReport.getId());

                // -------------------------------------------------------------------
                // 4. DISPARO DA IA (BEDROCK): Processa o PDF recém-baixado
                // -------------------------------------------------------------------
                log.info("🤖 Disparando extração financeira com Amazon Bedrock para {}", fileName);

                // Extrai texto usando apenas o nome do arquivo salvo em downloads/
                Path path = Paths.get(localPath);
                String onlyFileName = path.getFileName().toString();

                List<Document> documents = pdfService.readPdfAsDocuments(onlyFileName);
                String fullPdfText = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));

                // Pede o JSON pro Bedrock e mapeia
                FinancialReportDTO dto = extractor.extractMetrics(fullPdfText);

                // Converte quarter/year de String para Integer com segurança
                Integer parsedYear = Integer.parseInt(String.valueOf(meta.fileYear()));
                Integer parsedQuarter = extractQuarterNumber(String.valueOf(meta.fileQuarter()));

                FinancialReport financialEntity = mapper.toEntity(dto, "TOTVS", "TOTS3", parsedYear, parsedQuarter);
                financialRepository.save(financialEntity);
                // 🔔 Dispara notificação no Slack
                slackService.sendFinancialReportAlert(financialEntity);

                log.info("✅ Dados financeiros do Bedrock extraídos e salvos no PostgreSQL para o trimestre {}T{}", parsedQuarter, parsedYear);

            } catch (Exception e) {
                log.error("Erro ao sincronizar relatório {}: {}", meta.fileTitle(), e.getMessage(), e);
            }
        }

        return processedReports;
    }

    private Integer extractQuarterNumber(String fileQuarter) {
        if (fileQuarter == null) return 1;
        // Se vier "1T", "2T" ou só "1", limpa letras
        String cleaned = fileQuarter.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? 1 : Integer.parseInt(cleaned);
    }
}