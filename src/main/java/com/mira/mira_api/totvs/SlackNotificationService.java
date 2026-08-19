package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.model.FinancialReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class SlackNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationService.class);

    private final RestClient restClient;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    public SlackNotificationService() {
        this.restClient = RestClient.create();
    }

    public void sendFinancialReportAlert(FinancialReport report) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("⚠️ Slack Webhook URL não configurada. Notificação não enviada.");
            return;
        }

        try {
            // No SlackNotificationService.java:

            BigDecimal revenue = report.getRevenue() != null ? report.getRevenue().getRevenue() : null;
            BigDecimal revenueGrowth = report.getRevenue() != null ? report.getRevenue().getRevenueGrowth() : null;
            BigDecimal grossMargin = report.getProfitability() != null ? report.getProfitability().getGrossMargin() : null;
            BigDecimal ebitdaMargin = report.getProfitability() != null ? report.getProfitability().getEbitdaMargin() : null;
            BigDecimal freeCashFlow = report.getValuation() != null ? report.getValuation().getFreeCashFlow() : null;

// Formatações seguras com BigDecimal:
            String revenueText = revenue != null ? String.format("R$ %.1f M", revenue) : "N/D";
            String growthText = revenueGrowth != null ? String.format("%.1f%%", revenueGrowth) : "N/D";
            String grossMarginText = grossMargin != null ? String.format("%.1f%%", grossMargin) : "N/D";
            String ebitdaMarginText = ebitdaMargin != null ? String.format("%.1f%%", ebitdaMargin) : "N/D";
            String fcfText = freeCashFlow != null ? String.format("R$ %.1f M", freeCashFlow) : "N/D";

            // Monta payload no formato Block Kit do Slack
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
                                            Map.of("type", "mrkdwn", "text", "*Margem EBITDA:*\n" + ebitdaMarginText),
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

            // Dispara para o webhook do Slack
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("🔔 Alerta executivo enviado para o canal do Slack com sucesso!");

        } catch (Exception e) {
            log.error("Erro ao enviar notificação para o Slack: {}", e.getMessage(), e);
        }
    }
}