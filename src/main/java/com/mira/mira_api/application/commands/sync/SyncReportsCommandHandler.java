package com.mira.mira_api.application.commands.sync;

import com.mira.mira_api.application.events.ReportIngestedEvent;
import com.mira.mira_api.domain.FinancialReports.Service.CompanyReportService;
import com.mira.mira_api.domain.FinancialReports.Service.FinancialDataExtractor;
import com.mira.mira_api.domain.FinancialReports.dto.DocumentMeta;
import com.mira.mira_api.domain.FinancialReports.dto.FinancialReportDTO;
import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;
import com.mira.mira_api.domain.FinancialReports.entity.FinancialReportGeography;
import com.mira.mira_api.domain.FinancialReports.entity.FinancialReportVertical;
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

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        int targetYearValue = Integer.parseInt(String.valueOf(command.year()));
        log.info("Iniciando sincronização de {} ({}) para o ano {}",
                company.getName(), company.getTicker(), targetYearValue);

        List<DocumentMeta> metas = companyReportService.findReports(company, command.year());

        if (metas == null || metas.isEmpty()) {
            log.warn("⚠️ Nenhum documento encontrado para {} no ano {}.", company.getTicker(), targetYearValue);
            return;
        }

        // Agrupa os documentos por trimestre (ex: Q1, Q2) para processar cada trimestre isoladamente
        Map<Integer, List<DocumentMeta>> metasByQuarter = metas.stream()
                .filter(m -> {
                    int docYear = m.fileYear() != null ? Integer.parseInt(String.valueOf(m.fileYear())) : targetYearValue;
                    return docYear == targetYearValue;
                })
                .collect(Collectors.groupingBy(m -> extractQuarterNumber(String.valueOf(m.fileQuarter()))));

        for (Map.Entry<Integer, List<DocumentMeta>> entry : metasByQuarter.entrySet()) {
            int targetQuarter = entry.getKey();
            List<DocumentMeta> quarterMetas = entry.getValue();

            // Proteção contra duplicidade de trimestre
            if (financialReportRepository.existsByTickerAndYearAndQuarter(company.getTicker(), targetYearValue, targetQuarter)) {
                log.info("⏭️ Trimestre {}T{} de {} já processado anteriormente. Ignorando.",
                        targetQuarter, targetYearValue, company.getTicker());
                continue;
            }

            // 🚀 ORDENAÇÃO ATUALIZADA: Prioriza Apresentações, Earnings, Filings, ITRs e Releases para o topo
            quarterMetas.sort((m1, m2) -> {
                String t1 = m1.fileTitle() != null ? m1.fileTitle().toLowerCase() : "";
                String t2 = m2.fileTitle() != null ? m2.fileTitle().toLowerCase() : "";

                boolean isPrio1 = t1.contains("presentation")
                        || t1.contains("apresentação")
                        || t1.contains("slides")
                        || t1.contains("release")
                        || t1.contains("earnings")
                        || t1.contains("filing")
                        || t1.contains("itr");

                boolean isPrio2 = t2.contains("presentation")
                        || t2.contains("apresentação")
                        || t2.contains("slides")
                        || t2.contains("release")
                        || t2.contains("earnings")
                        || t2.contains("filing")
                        || t2.contains("itr");

                if (isPrio1 && !isPrio2) return -1;
                if (!isPrio1 && isPrio2) return 1;
                return 0;
            });

            StringBuilder combinedFullText = new StringBuilder();
            String primaryTitleFound = "";
            String firstLinkId = null;
            String firstFileUrl = null;
            String firstLocalPath = null;

            for (DocumentMeta meta : quarterMetas) {
                String titleLower = meta.fileTitle() != null ? meta.fileTitle().toLowerCase() : "";

                if (isPurelyIrrelevantDocument(titleLower)) {
                    log.info("⏭️ Ignorando arquivo irrelevante: {}", meta.fileTitle());
                    continue;
                }

                String linkId = meta.downloadLinkId() != null ? meta.downloadLinkId() : meta.fileUrl();
                if (firstLinkId == null) {
                    firstLinkId = linkId;
                    firstFileUrl = meta.fileUrl();
                    primaryTitleFound = meta.fileTitle();
                }

                try {
                    String safeFileName = (company.getTicker() + "_" + targetQuarter + "T_" + meta.fileTitle())
                            .replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
                    File downloadedFile = reportDownloader.downloadPdf(meta.fileUrl(), safeFileName);
                    String localFilePath = downloadedFile.getAbsolutePath();

                    if (firstLocalPath == null) {
                        firstLocalPath = localFilePath;
                    }

                    String pdfText = springAiPdfService.extractFullText(localFilePath);

                    combinedFullText.append("\n\n========================================\n");
                    combinedFullText.append("DOCUMENTO: ").append(meta.fileTitle()).append("\n");
                    combinedFullText.append("========================================\n\n");
                    combinedFullText.append(pdfText);

                    log.info("📄 Texto acumulado para {}T{}: [{}]", targetQuarter, targetYearValue, meta.fileTitle());

                } catch (Exception e) {
                    log.error("Erro ao descarregar/processar o documento {}: {}", meta.fileTitle(), e.getMessage(), e);
                }
            }

            if (combinedFullText.isEmpty()) {
                continue;
            }

            try {
                ReportEntity reportEntity = new ReportEntity(
                        company.getName(),
                        primaryTitleFound,
                        targetQuarter,
                        targetYearValue,
                        firstLinkId,
                        firstFileUrl,
                        firstLocalPath
                );
                reportRepository.save(reportEntity);

                log.info("🤖 A enviar texto combinado do trimestre {}T{} para o Claude...", targetQuarter, targetYearValue);

                FinancialReportDTO dto = extractor.extractMetrics(combinedFullText.toString(), company.getName(), company.getTicker());

                FinancialReport financialReport = mapper.toEntity(dto, company.getName(), company.getTicker(), targetYearValue, targetQuarter);

                mapListsToEntity(financialReport, dto);
                applyCalculatedMetrics(financialReport, dto);

                FinancialReport savedReport = financialReportRepository.save(financialReport);

                eventPublisher.publishEvent(new ReportIngestedEvent(savedReport));
                log.info("Relatório {}T{} processado com sucesso!", targetQuarter, targetYearValue);

            } catch (Exception e) {
                log.error("Erro ao extrair métricas do trimestre {}T{}: {}", targetQuarter, targetYearValue, e.getMessage(), e);
            }
        }

        log.info("Sincronização de {} para o ano {} concluída!", company.getTicker(), targetYearValue);
    }

    private void mapListsToEntity(FinancialReport financialReport, FinancialReportDTO dto) {
        if (dto.industryVerticals() != null && (financialReport.getIndustryVerticals() == null || financialReport.getIndustryVerticals().isEmpty())) {
            for (var vDto : dto.industryVerticals()) {
                FinancialReportVertical vertical = new FinancialReportVertical();
                vertical.setVerticalName(vDto.verticalName());
                vertical.setRevenue(vDto.revenue());
                vertical.setPercentage(vDto.percentage());
                financialReport.addVertical(vertical);
            }
        }

        if (dto.geographicRevenue() != null && (financialReport.getGeographicRevenue() == null || financialReport.getGeographicRevenue().isEmpty())) {
            for (var gDto : dto.geographicRevenue()) {
                FinancialReportGeography geo = new FinancialReportGeography();
                geo.setRegionName(gDto.regionName());
                geo.setRevenue(gDto.revenue());
                geo.setPercentage(gDto.percentage());
                financialReport.addGeography(geo);
            }
        }
    }

    private void applyCalculatedMetrics(FinancialReport financialReport, FinancialReportDTO dto) {
        sanitizeZeros(financialReport);

        if (financialReport.getProfitability() != null && financialReport.getRevenue() != null) {
            BigDecimal revenueGrowth = financialReport.getRevenue().getRevenueGrowth();
            BigDecimal ebitdaMargin = financialReport.getProfitability().getEbitdaMargin();

            if (revenueGrowth != null && ebitdaMargin != null) {
                BigDecimal ruleOf40 = revenueGrowth.add(ebitdaMargin);
                financialReport.getProfitability().setRuleOf40Score(ruleOf40);
            }
        }

        if (financialReport.getValuation() != null && financialReport.getRevenue() != null && financialReport.getProfitability() != null) {
            BigDecimal freeCashFlow = financialReport.getValuation().getFreeCashFlow();
            BigDecimal netRevenue = financialReport.getRevenue().getRevenue();

            if (freeCashFlow != null && netRevenue != null && netRevenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal fcfMargin = freeCashFlow
                        .divide(netRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                financialReport.getProfitability().setFcfMargin(fcfMargin);
            }
        }

        if (financialReport.getValuation() != null && financialReport.getRevenue() != null && financialReport.getProfitability() != null) {
            BigDecimal freeCashFlow = financialReport.getValuation().getFreeCashFlow();
            BigDecimal netRevenue = financialReport.getRevenue().getRevenue();
            BigDecimal ebitdaMargin = financialReport.getProfitability().getEbitdaMargin();

            if (freeCashFlow != null && netRevenue != null && ebitdaMargin != null && ebitdaMargin.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ebitdaAbsolute = netRevenue
                        .multiply(ebitdaMargin)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                if (ebitdaAbsolute.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal fcfConversion = freeCashFlow
                            .divide(ebitdaAbsolute, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    financialReport.getProfitability().setFreeCashFlowConversion(fcfConversion);
                }
            }
        }

        if (financialReport.getRevenue() != null && financialReport.getOperational() != null) {
            BigDecimal netRevenue = financialReport.getRevenue().getRevenue();
            Integer employees = financialReport.getOperational().getEmployees();

            if (netRevenue != null && employees != null && employees > 0) {
                BigDecimal revenuePerProf = netRevenue
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(employees), 2, RoundingMode.HALF_UP);

                financialReport.getOperational().setRevenuePerProfessional(revenuePerProf);
            } else {
                financialReport.getOperational().setRevenuePerProfessional(null);
            }
        }
    }

    private void sanitizeZeros(FinancialReport financialReport) {
        if (financialReport.getProfitability() != null) {
            var prof = financialReport.getProfitability();
            if (prof.getGrossMargin() != null && prof.getGrossMargin().compareTo(BigDecimal.ZERO) == 0) {
                prof.setGrossMargin(null);
            }
            if (prof.getEbitdaMargin() != null && prof.getEbitdaMargin().compareTo(BigDecimal.ZERO) == 0) {
                prof.setEbitdaMargin(null);
            }
            if (prof.getNetMargin() != null && prof.getNetMargin().compareTo(BigDecimal.ZERO) == 0) {
                prof.setNetMargin(null);
            }
            if (prof.getFcfMargin() != null && prof.getFcfMargin().compareTo(BigDecimal.ZERO) == 0) {
                prof.setFcfMargin(null);
            }
        }
    }

    private int extractQuarterNumber(String quarterText) {
        if (quarterText == null) return 1;
        String digits = quarterText.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 1 : Integer.parseInt(digits);
    }

    private boolean isPurelyIrrelevantDocument(String titleLower) {
        return titleLower.contains("transcript")
                || titleLower.contains("audio")
                || titleLower.contains("webcast")
                || titleLower.contains("spreadsheet")
                || titleLower.contains("planilha");
    }
}