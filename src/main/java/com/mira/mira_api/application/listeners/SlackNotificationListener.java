package com.mira.mira_api.application.listeners;

import com.mira.mira_api.application.events.ReportIngestedEvent;
import com.mira.mira_api.domain.FinancialReports.entity.FinancialReport;
import com.mira.mira_api.domain.company.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class SlackNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationListener.class);

    private final RestClient restClient;
    private final CompanyRepository companyRepository;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public SlackNotificationListener(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
        this.restClient = RestClient.create();
    }

    /**
     * Ouve o evento disparado após o relatório ser persistido no banco
     * Executa em background (Thread separada) sem travar o pipeline principal
     */
    @Async
    @EventListener
    public void onReportIngested(ReportIngestedEvent event) {
        sendFinancialReportAlert(event.report());
    }

    public void sendFinancialReportAlert(FinancialReport report) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("⚠️ Slack Webhook URL não configurada. Notificação não enviada.");
            return;
        }

        try {
            String currencySymbol = resolveCurrencySymbol(report.getTicker());

            BigDecimal revenue = report.getRevenue() != null ? report.getRevenue().getRevenue() : null;
            BigDecimal revenueGrowth = report.getRevenue() != null ? report.getRevenue().getRevenueGrowth() : null;
            BigDecimal grossMargin = report.getProfitability() != null ? report.getProfitability().getGrossMargin() : null;
            BigDecimal ebitdaMargin = report.getProfitability() != null ? report.getProfitability().getEbitdaMargin() : null;
            BigDecimal freeCashFlow = report.getValuation() != null ? report.getValuation().getFreeCashFlow() : null;

            String revenueText = revenue != null ? String.format("%s %.1f M", currencySymbol, revenue) : "N/D";
            String growthText = revenueGrowth != null ? String.format("%.1f%%", revenueGrowth) : "N/D";
            String grossMarginText = grossMargin != null ? String.format("%.1f%%", grossMargin) : "N/D";
            String ebitdaMarginText = ebitdaMargin != null ? String.format("%.1f%%", ebitdaMargin) : "N/D";
            String fcfText = freeCashFlow != null ? String.format("%s %.1f M", currencySymbol, freeCashFlow) : "N/D";

            Map<String, Object> payload = Map.of(
                    "blocks", List.of(
                            Map.of(
                                    "type", "header",
                                    "text", Map.of(
                                            "type", "plain_text",
                                            "text", "📊 Novo Release Processado: " + report.getCompany() + " (" + report.getQuarter() + "T" + report.getYear() + ")",
                                            "emoji", true
                                    )
                            ),
                            Map.of(
                                    "type", "section",
                                    "fields", List.of(
                                            Map.of("type", "mrkdwn", "text", "*Ticker:*\n`" + report.getTicker() + "`"),
                                            Map.of("type", "mrkdwn", "text", "*Receita Líquida:*\n" + revenueText),
                                            Map.of("type", "mrkdwn", "text", "*Crescimento YoY:*\n" + growthText),
                                            Map.of("type", "mrkdwn", "text", "*Margem Bruta:*\n" + grossMarginText),
                                            Map.of("type", "mrkdwn", "text", "*Margem EBITDA / Operacional:*\n" + ebitdaMarginText),
                                            Map.of("type", "mrkdwn", "text", "*Fluxo de Caixa Livre:*\n" + fcfText)
                                    )
                            ),
                            Map.of(
                                    "type", "context",
                                    "elements", List.of(
                                            Map.of("type", "mrkdwn", "text", "🤖 _Extração automática via Claude 3.5 Bedrock • Dados gravados no PostgreSQL_")
                                    )
                            )
                    )
            );

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("🔔 Alerta executivo de {} ({}) enviado para o Slack com sucesso!", report.getCompany(), report.getTicker());

        } catch (Exception e) {
            log.error("Erro ao enviar notificação para o Slack: {}", e.getMessage(), e);
        }
    }

    private String resolveCurrencySymbol(String ticker) {
        if (ticker == null) return "R$";

        // Tenta buscar da entidade Company persistida no banco
        return companyRepository.findByTicker(ticker)
                .map(company -> switch (company.getCurrency() != null ? company.getCurrency().toUpperCase() : "") {
                    case "USD" -> "$";
                    case "GBP" -> "£";
                    case "EUR" -> "€";
                    default -> "R$";
                })
                .orElseGet(() -> fallbackCurrencyByTicker(ticker));
    }

    private String fallbackCurrencyByTicker(String ticker) {
        String t = ticker.trim().toUpperCase();
        if (t.endsWith("3") || t.endsWith("4") || t.endsWith("11")) return "R$";
        if ("DAVA".equalsIgnoreCase(t)) return "£";
        if ("ACN".equalsIgnoreCase(t) || "CINT".equalsIgnoreCase(t)) return "$";
        return "R$";
    }
}