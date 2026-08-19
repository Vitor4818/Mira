package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.dto.FinancialReportDTO;
import com.mira.mira_api.totvs.model.FinancialReport;
import com.mira.mira_api.totvs.repository.FinancialReportRepository;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FinancialDataService {

    private final SpringAiPdfService pdfService;
    private final FinancialDataExtractor extractor;
    private final FinancialReportMapper mapper;
    private final FinancialReportRepository repository;

    public FinancialDataService(
            SpringAiPdfService pdfService,
            FinancialDataExtractor extractor,
            FinancialReportMapper mapper,
            FinancialReportRepository repository
    ) {
        this.pdfService = pdfService;
        this.extractor = extractor;
        this.mapper = mapper;
        this.repository = repository;
    }

    @Transactional
    public FinancialReport ingestIfNew(String fileName, String company, String ticker, Integer year, Integer quarter) {
        // 🛑 TRAVA DE SEGURANÇA: Verifica se o relatório já existe no PostgreSQL
        boolean exists = repository.existsByCompanyAndYearAndQuarter(company, year, quarter);
        if (exists) {
            System.out.println("⚠️ Relatório " + company + " (" + quarter + "T" + year + ") já está cadastrado no banco. Abortando chamada ao Bedrock para economizar tokens.");
            throw new IllegalArgumentException("O relatório de " + company + " referente a " + quarter + "T" + year + " já foi processado e existe no banco de dados.");
        }

        System.out.println("🚀 Relatório inédito. Iniciando pipeline de extração com Amazon Bedrock...");

        // 1. Extrai todo o texto do PDF usando Spring AI Reader
        List<Document> documents = pdfService.readPdfAsDocuments(fileName);
        String fullPdfText = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 2. Envia para o Amazon Bedrock / Claude extrair o DTO estruturado
        FinancialReportDTO dto = extractor.extractMetrics(fullPdfText);

        // 3. Mapeia para a Entidade pai FinancialReport e filhos
        FinancialReport reportEntity = mapper.toEntity(dto, company, ticker, year, quarter);

        // 4. Persiste no PostgreSQL em cascata
        return repository.save(reportEntity);
    }
}