package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.model.*;
import com.mira.mira_api.totvs.repository.FinancialReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/totvs")
public class TotvsController {

    private final TotvsReportSyncService syncService;
    private final SlackNotificationService slackNotificationService;
    private final FinancialReportRepository financialReportRepository;

    public TotvsController(
            TotvsReportSyncService syncService,
            SlackNotificationService slackNotificationService,
            FinancialReportRepository financialReportRepository
    ) {
        this.syncService = syncService;
        this.slackNotificationService = slackNotificationService;
        this.financialReportRepository = financialReportRepository;
    }

    /**
     * Endpoint Mestre:
     * Chama a API do MZIQ -> Baixa PDFs novos -> Roda Bedrock -> Salva métricas financeiras
     */
    @PostMapping("/sync")
    public ResponseEntity<List<ReportEntity>> syncReports(@RequestParam(defaultValue = "2026") String year) {
        List<ReportEntity> reports = syncService.syncReports(year);
        return ResponseEntity.ok(reports);
    }

    /**
     * Endpoint de Teste do Slack:
     * Dispara o card executivo para o canal configurado no webhook.
     */
    @PostMapping("/test-slack-card")
    public ResponseEntity<String> testSlackCard() {
        List<FinancialReport> reports = financialReportRepository.findAll();

        FinancialReport reportToSend;

        if (!reports.isEmpty()) {
            // Pega o último relatório salvo no banco
            reportToSend = reports.get(reports.size() - 1);
        } else {
            // Cria um payload simulado caso o banco esteja vazio
            reportToSend = new FinancialReport();
            reportToSend.setCompany("TOTVS");
            reportToSend.setTicker("TOTS3");
            reportToSend.setYear(2024);
            reportToSend.setQuarter(3);

            Revenue rev = new Revenue();
            rev.setRevenue(new BigDecimal("1506.9"));
            rev.setRevenueGrowth(new BigDecimal("16.3"));
            reportToSend.setRevenue(rev);

            Profitability prof = new Profitability();
            prof.setGrossMargin(new BigDecimal("72.7"));
            prof.setEbitdaMargin(new BigDecimal("27.1"));
            reportToSend.setProfitability(prof);

            Valuation val = new Valuation();
            val.setFreeCashFlow(new BigDecimal("317.1"));
            reportToSend.setValuation(val);
        }

        slackNotificationService.sendFinancialReportAlert(reportToSend);

        return ResponseEntity.ok("Card do Slack enviado com sucesso para "
                + reportToSend.getCompany() + " (" + reportToSend.getQuarter() + "T" + reportToSend.getYear() + ")!");
    }
}