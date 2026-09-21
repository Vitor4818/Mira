package com.mira.mira_api.application.commands.sync;

import com.mira.mira_api.application.events.ReportIngestedEvent;
import com.mira.mira_api.domain.FinancialReports.Service.CompanyReportService;
import com.mira.mira_api.domain.FinancialReports.Service.FinancialDataExtractor;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.dto.FinancialReportDTO;
import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;
import com.mira.mira_api.domain.FinancialReports.entity.ReportEntity;
import com.mira.mira_api.domain.FinancialReports.mapper.FinancialReportMapper;
import com.mira.mira_api.domain.company.entity.Company;
import com.mira.mira_api.domain.company.repository.CompanyRepository;
import com.mira.mira_api.infrastructure.downloader.ReportDownloader;
import com.mira.mira_api.infrastructure.pdf.SpringAiPdfService;
import com.mira.mira_api.infrastructure.persistence.jpa.FinancialReports.FinancialReportRepository;
import com.mira.mira_api.infrastructure.persistence.jpa.FinancialReports.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SyncReportsCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SyncReportsCommandHandler.class);

    private final CompanyRepository companyRepository;
    private final CompanyReportService companyReportService;
    private final ReportRepository reportRepository;
    private final FinancialReportRepository financialReportRepository;
    private final ReportDownloader reportDownloader;
    private final SpringAiPdfService springAiPdfService;
    private final FinancialDataExtractor extractor;
    private final FinancialReportMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    public SyncReportsCommandHandler(
            CompanyRepository companyRepository,
            CompanyReportService companyReportService,
            ReportRepository reportRepository,
            FinancialReportRepository financialReportRepository,
            ReportDownloader reportDownloader,
            SpringAiPdfService springAiPdfService,
            FinancialDataExtractor extractor,
            FinancialReportMapper mapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.companyRepository = companyRepository;
        this.companyReportService = companyReportService;
        this.reportRepository = reportRepository;
        this.financialReportRepository = financialReportRepository;
        this.reportDownloader = reportDownloader;
        this.springAiPdfService = springAiPdfService;
        this.extractor = extractor;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handle(SyncReportsCommand command) {
        Company company = companyRepository.findByTicker(command.ticker().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada para o ticker: " + command.ticker()));

        log.info("Iniciando sincronização de {} ({}) para o ano {}", company.getName(), company.getTicker(), command.year());

        List<DocumentMeta> metas = companyReportService.findReports(company, command.year());

        for (DocumentMeta meta : metas) {
            String titleLower = meta.fileTitle() != null ? meta.fileTitle().toLowerCase() : "";

            // 1. Filtro contra apresentações e arquivos que não contêm demonstrativos contábeis
            if (isSecondaryDocument(titleLower)) {
                log.info("⏭️ Ignorando arquivo não contábil/apresentação: {}", meta.fileTitle());
                continue;
            }

            int year = Integer.parseInt(String.valueOf(meta.fileYear()));
            int quarter = extractQuarterNumber(String.valueOf(meta.fileQuarter()));

            // 2. Proteção contra duplicidade de trimestre para a mesma empresa
            if (financialReportRepository.existsByTickerAndYearAndQuarter(company.getTicker(), year, quarter)) {
                log.info("⏭️ Trimestre {}T{} de {} já processado anteriormente. Ignorando: {}",
                        quarter, year, company.getTicker(), meta.fileTitle());
                continue;
            }

            String linkId = meta.downloadLinkId() != null ? meta.downloadLinkId() : meta.fileUrl();

            if (reportRepository.existsByDownloadLinkId(linkId)) {
                log.info("Relatório já baixado anteriormente: {}", meta.fileTitle());
                continue;
            }

            try {
                String safeFileName = (company.getTicker() + "_" + meta.fileTitle()).replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
                String localFilePath = reportDownloader.downloadPdf(meta.fileUrl(), safeFileName);

                ReportEntity reportEntity = new ReportEntity(
                        company.getName(),
                        meta.fileTitle(),
                        meta.fileQuarter(),
                        meta.fileYear(),
                        linkId,
                        meta.fileUrl(),
                        localFilePath
                );
                reportRepository.save(reportEntity);

                // 3. Extração do texto de 100% das páginas via Spring AI
                String fullPdfText = springAiPdfService.extractFullText(localFilePath);

                // 4. Envio de todo o conteúdo para o Claude 3.5 Sonnet
                FinancialReportDTO dto = extractor.extractMetrics(fullPdfText, company.getName(), company.getTicker());

                FinancialReport financialReport = mapper.toEntity(dto, company.getName(), company.getTicker(), year, quarter);
                FinancialReport savedReport = financialReportRepository.save(financialReport);

                eventPublisher.publishEvent(new ReportIngestedEvent(savedReport));
                log.info("Relatório {}T{} processado com sucesso via texto integral!", quarter, year);

            } catch (Exception e) {
                log.error("Erro ao processar relatório {}: {}", meta.fileTitle(), e.getMessage(), e);
            }
        }
    }

    private boolean isSecondaryDocument(String titleLower) {
        return titleLower.contains("presentation")
                || titleLower.contains("apresentação")
                || titleLower.contains("apresentacao")
                || titleLower.contains("slides")
                || titleLower.contains("transcript")
                || titleLower.contains("audio")
                || titleLower.contains("webcast")
                || titleLower.contains("spreadsheet")
                || titleLower.contains("planilha")
                || titleLower.contains("tabela");
    }

    private int extractQuarterNumber(String quarterText) {
        if (quarterText == null) return 1;
        String digits = quarterText.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 1 : Integer.parseInt(digits);
    }
}