package com.mira.mira_api.api;

import com.mira.mira_api.application.commands.ingest.IngestReportCommand;
import com.mira.mira_api.application.commands.ingest.IngestReportCommandHandler;
import com.mira.mira_api.application.commands.sync.SyncReportsCommand;
import com.mira.mira_api.application.commands.sync.SyncReportsCommandHandler;
import com.mira.mira_api.application.listeners.SlackNotificationListener;
import com.mira.mira_api.domain.FinancialReports.entity.*;
import com.mira.mira_api.infrastructure.persistence.jpa.FinancialReports.FinancialReportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/intelligence")
public class FinancialIntelligenceController {

    private final SyncReportsCommandHandler syncHandler;
    private final IngestReportCommandHandler ingestHandler;
    private final SlackNotificationListener slackNotificationListener;
    private final FinancialReportRepository financialReportRepository;

    public FinancialIntelligenceController(
            SyncReportsCommandHandler syncHandler,
            IngestReportCommandHandler ingestHandler,
            SlackNotificationListener slackNotificationListener,
            FinancialReportRepository financialReportRepository
    ) {
        this.syncHandler = syncHandler;
        this.ingestHandler = ingestHandler;
        this.slackNotificationListener = slackNotificationListener;
        this.financialReportRepository = financialReportRepository;
    }

    /**
     * COMMAND: Sincronização automática para qualquer empresa via Ticker.
     * Exemplo: POST /api/v1/intelligence/sync?ticker=TOTS3&year=2026
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncReports(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "2026") String year
    ) {
        syncHandler.handle(new SyncReportsCommand(ticker, year));
        return ResponseEntity.ok(String.format("Sincronização dos relatórios de %s (%s) concluída com sucesso!", ticker.toUpperCase(), year));
    }

    /**
     * COMMAND: Ingestão pontual de um PDF já salvo em disco.
     */
    @PostMapping("/ingest")
    public ResponseEntity<FinancialReport> ingestReport(@RequestBody IngestReportCommand command) {
        FinancialReport report = ingestHandler.handle(command);
        return ResponseEntity.ok(report);
    }

    /**
     * Endpoint utilitário para testar o card do Slack.
     * Pode passar o ticker opcionalmente: POST /api/v1/intelligence/test-slack-card?ticker=TOTS3
     */
    @PostMapping("/test-slack-card")
    public ResponseEntity<String> testSlackCard(@RequestParam(required = false) String ticker) {
        List<FinancialReport> reports = (ticker != null && !ticker.isBlank())
                ? financialReportRepository.findAll().stream()
                .filter(r -> ticker.equalsIgnoreCase(r.getTicker()))
                .toList()
                : financialReportRepository.findAll();

        FinancialReport reportToSend;

        if (!reports.isEmpty()) {
            reportToSend = reports.get(reports.size() - 1);
        } else {
            reportToSend = new FinancialReport();
            reportToSend.setCompany(ticker != null ? ticker.toUpperCase() : "EMPRESA TESTE");
            reportToSend.setTicker(ticker != null ? ticker.toUpperCase() : "TEST3");
            reportToSend.setYear(2025);
            reportToSend.setQuarter(4);

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

        slackNotificationListener.sendFinancialReportAlert(reportToSend);

        return ResponseEntity.ok("Card do Slack disparado para "
                + reportToSend.getCompany() + " (" + reportToSend.getQuarter() + "T" + reportToSend.getYear() + ")!");
    }
}