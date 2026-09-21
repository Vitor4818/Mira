package com.mira.mira_api.application.commands.ingest;

import com.mira.mira_api.application.events.ReportIngestedEvent;
import com.mira.mira_api.domain.FinancialReports.Service.FinancialDataExtractor;
import com.mira.mira_api.domain.FinancialReports.mapper.FinancialReportMapper;
import com.mira.mira_api.infrastructure.pdf.SpringAiPdfService;
import com.mira.mira_api.domain.FinancialReports.dto.FinancialReportDTO;
import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;
import com.mira.mira_api.infrastructure.persistence.jpa.FinancialReports.FinancialReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IngestReportCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(IngestReportCommandHandler.class);

    private final SpringAiPdfService pdfService;
    private final FinancialDataExtractor extractor;
    private final FinancialReportMapper mapper;
    private final FinancialReportRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public IngestReportCommandHandler(
            SpringAiPdfService pdfService,
            FinancialDataExtractor extractor,
            FinancialReportMapper mapper,
            FinancialReportRepository repository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.pdfService = pdfService;
        this.extractor = extractor;
        this.mapper = mapper;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FinancialReport handle(IngestReportCommand command) {
        // 1. Trava de segurança no banco
        boolean exists = repository.existsByCompanyAndYearAndQuarter(
                command.company(), command.year(), command.quarter()
        );

        if (exists) {
            log.warn("⚠️ Relatório {} ({}T{}) já cadastrado. Abortando chamada ao Bedrock.",
                    command.company(), command.quarter(), command.year());
            throw new IllegalArgumentException(String.format("O relatório de %s referente a %sT%s já foi processado.",
                    command.company(), command.quarter(), command.year()));
        }

        log.info("🚀 Relatório inédito. Iniciando pipeline de extração com Amazon Bedrock para {}...", command.ticker());

        // 2. Extrai texto do PDF
        List<Document> documents = pdfService.readPdfAsDocuments(command.fileName());
        String fullPdfText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 3. Extrai DTO via Bedrock passando empresa e ticker dinâmicos
        FinancialReportDTO dto = extractor.extractMetrics(fullPdfText, command.company(), command.ticker());

        // 4. Mapeia e persiste no PostgreSQL
        FinancialReport reportEntity = mapper.toEntity(
                dto, command.company(), command.ticker(), command.year(), command.quarter()
        );
        FinancialReport savedReport = repository.save(reportEntity);

        // 5. Dispara evento assíncrono (Slack Listener consome)
        eventPublisher.publishEvent(new ReportIngestedEvent(savedReport));

        return savedReport;
    }
}