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
import java.util.ArrayList;
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

            // 1. Receitas e Valuation
            BigDecimal revenue = report.getRevenue() != null ? report.getRevenue().getRevenue() : null;
            BigDecimal revenueGrowth = report.getRevenue() != null ? report.getRevenue().getRevenueGrowth() : null;
            BigDecimal freeCashFlow = report.getValuation() != null ? report.getValuation().getFreeCashFlow() : null;
            BigDecimal cashAndEquivalents = report.getValuation() != null ? report.getValuation().getCashAndEquivalents() : null;
            BigDecimal netDebt = report.getValuation() != null ? report.getValuation().getNetDebt() : null;

            // 2. Rentabilidade
            BigDecimal grossMargin = report.getProfitability() != null ? report.getProfitability().getGrossMargin() : null;
            BigDecimal ebitdaMargin = report.getProfitability() != null ? report.getProfitability().getEbitdaMargin() : null;
            BigDecimal netMargin = report.getProfitability() != null ? report.getProfitability().getNetMargin() : null;
            BigDecimal fcfMargin = report.getProfitability() != null ? report.getProfitability().getFcfMargin() : null;
            BigDecimal sgaToRevenue = report.getProfitability() != null ? report.getProfitability().getSgaToRevenue() : null;

            // 3. Operacional e Equipa (Tratamento para ignorar zeros irrelevantes)
            BigDecimal utilizationRate = (report.getOperational() != null && report.getOperational().getUtilizationRate() != null && report.getOperational().getUtilizationRate().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getOperational().getUtilizationRate() : null;
            BigDecimal attrition = (report.getOperational() != null && report.getOperational().getAttrition() != null && report.getOperational().getAttrition().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getOperational().getAttrition() : null;
            Integer employees = (report.getOperational() != null && report.getOperational().getEmployees() != null && report.getOperational().getEmployees() > 0)
                    ? report.getOperational().getEmployees() : null;
            BigDecimal revenuePerProfessional = (report.getOperational() != null && report.getOperational().getRevenuePerProfessional() != null && report.getOperational().getRevenuePerProfessional().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getOperational().getRevenuePerProfessional() : null;

            // 4. Clientes e Retenção (Tratamento para ignorar zeros irrelevantes)
            BigDecimal topClientConc = (report.getClients() != null && report.getClients().getTopClientConcentration() != null && report.getClients().getTopClientConcentration().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getClients().getTopClientConcentration() : null;
            BigDecimal top5ClientsConc = (report.getClients() != null && report.getClients().getTop5ClientsConcentration() != null && report.getClients().getTop5ClientsConcentration().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getClients().getTop5ClientsConcentration() : null;
            BigDecimal top10ClientsConc = (report.getClients() != null && report.getClients().getTop10ClientsConcentration() != null && report.getClients().getTop10ClientsConcentration().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getClients().getTop10ClientsConcentration() : null;
            BigDecimal netRetentionRate = (report.getClients() != null && report.getClients().getNetRetentionRate() != null && report.getClients().getNetRetentionRate().compareTo(BigDecimal.ZERO) > 0)
                    ? report.getClients().getNetRetentionRate() : null;

            // Textos formatados
            String revenueText = revenue != null ? String.format("%s %.1f M", currencySymbol, revenue) : "N/D";
            String growthText = revenueGrowth != null ? String.format("%.1f%%", revenueGrowth) : "N/D";
            String fcfText = freeCashFlow != null ? String.format("%s %.1f M", currencySymbol, freeCashFlow) : "N/D";
            String cashText = cashAndEquivalents != null ? String.format("%s %.1f M", currencySymbol, cashAndEquivalents) : "N/D";
            String netDebtText = netDebt != null ? String.format("%s %.1f M", currencySymbol, netDebt) : "N/D";

            String grossMarginText = grossMargin != null ? String.format("%.1f%%", grossMargin) : "N/D";
            String ebitdaMarginText = ebitdaMargin != null ? String.format("%.1f%%", ebitdaMargin) : "N/D";
            String netMarginText = netMargin != null ? String.format("%.1f%%", netMargin) : "N/D";
            String fcfMarginText = fcfMargin != null ? String.format("%.1f%%", fcfMargin) : "N/D";
            String sgaText = sgaToRevenue != null ? String.format("%.1f%%", sgaToRevenue) : "N/D";

            String utilText = utilizationRate != null ? String.format("%.1f%%", utilizationRate) : "Não Divulgado";
            String attritionText = attrition != null ? String.format("%.1f%%", attrition) : "Não Divulgado";
            String headcountText = employees != null ? String.format("%,d", employees) : "Não Divulgado";
            String revPerProfText = revenuePerProfessional != null ? String.format("%s %.1f k", currencySymbol, revenuePerProfessional) : "Não Divulgado";

            String top1Text = topClientConc != null ? String.format("%.1f%%", topClientConc) : "Não Divulgado";
            String top5Text = top5ClientsConc != null ? String.format("%.1f%%", top5ClientsConc) : "Não Divulgado";
            String top10Text = top10ClientsConc != null ? String.format("%.1f%%", top10ClientsConc) : "Não Divulgado";
            String nrrText = netRetentionRate != null ? String.format("%.1f%%", netRetentionRate) : "Não Divulgado";

            // Formatação das Verticais de Indústria
            StringBuilder verticalsText = new StringBuilder();
            if (report.getIndustryVerticals() != null && !report.getIndustryVerticals().isEmpty()) {
                for (var vertical : report.getIndustryVerticals()) {
                    String revVal = vertical.getRevenue() != null ? String.format("%s %.2f M", currencySymbol, vertical.getRevenue()) : "";
                    String percVal = vertical.getPercentage() != null ? String.format(" (%.1f%%)", vertical.getPercentage()) : "";
                    verticalsText.append(String.format("• *%s*: %s%s\n", vertical.getVerticalName(), revVal, percVal));
                }
            } else {
                verticalsText.append("Não Divulgado");
            }

            // Formatação da Receita Geográfica
            StringBuilder geoText = new StringBuilder();
            if (report.getGeographicRevenue() != null && !report.getGeographicRevenue().isEmpty()) {
                for (var geo : report.getGeographicRevenue()) {
                    String revVal = geo.getRevenue() != null ? String.format("%s %.2f M", currencySymbol, geo.getRevenue()) : "";
                    String percVal = geo.getPercentage() != null ? String.format(" (%.1f%%)", geo.getPercentage()) : "";
                    geoText.append(String.format("• *%s*: %s%s\n", geo.getRegionName(), revVal, percVal));
                }
            } else {
                geoText.append("Não Divulgado");
            }

            // Construção dinâmica dos Blocos do Slack
            List<Map<String, Object>> blocks = new ArrayList<>();

            // Header
            blocks.add(Map.of(
                    "type", "header",
                    "text", Map.of(
                            "type", "plain_text",
                            "text", "📊 Relatório Consolidado: " + report.getCompany() + " (" + report.getQuarter() + "T" + report.getYear() + ")",
                            "emoji", true
                    )
            ));

            // Bloco 1: Finanças & Valuation
            blocks.add(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text", "*💰 Finanças, Crescimento & Caixa*")
            ));
            blocks.add(Map.of(
                    "type", "section",
                    "fields", List.of(
                            Map.of("type", "mrkdwn", "text", "*Ticker:*\n`" + report.getTicker() + "`"),
                            Map.of("type", "mrkdwn", "text", "*Receita Líquida:*\n" + revenueText),
                            Map.of("type", "mrkdwn", "text", "*Crescimento YoY:*\n" + growthText),
                            Map.of("type", "mrkdwn", "text", "*Fluxo de Caixa Livre:*\n" + fcfText),
                            Map.of("type", "mrkdwn", "text", "*Caixa e Equivalentes:*\n" + cashText),
                            Map.of("type", "mrkdwn", "text", "*Dívida Líquida:*\n" + netDebtText)
                    )
            ));

            blocks.add(Map.of("type", "divider"));

            // Bloco 2: Rentabilidade
            blocks.add(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text", "*📈 Rentabilidade & Margens*")
            ));
            blocks.add(Map.of(
                    "type", "section",
                    "fields", List.of(
                            Map.of("type", "mrkdwn", "text", "*Margem Bruta:*\n" + grossMarginText),
                            Map.of("type", "mrkdwn", "text", "*Margem EBITDA/Op:*\n" + ebitdaMarginText),
                            Map.of("type", "mrkdwn", "text", "*Margem Líquida:*\n" + netMarginText),
                            Map.of("type", "mrkdwn", "text", "*Margem FCF:*\n" + fcfMarginText),
                            Map.of("type", "mrkdwn", "text", "*SG&A / Receita:*\n" + sgaText)
                    )
            ));

            // Bloco 3: Operacional & Equipa
            boolean hasOperationalData = utilizationRate != null || attrition != null || employees != null || revenuePerProfessional != null;
            if (hasOperationalData) {
                blocks.add(Map.of("type", "divider"));
                blocks.add(Map.of(
                        "type", "section",
                        "text", Map.of("type", "mrkdwn", "text", "*⚙️ Operacional, Equipa & Produtividade*")
                ));
                blocks.add(Map.of(
                        "type", "section",
                        "fields", List.of(
                                Map.of("type", "mrkdwn", "text", "*Utilization Rate:*\n" + utilText),
                                Map.of("type", "mrkdwn", "text", "*Attrition / Turnover:*\n" + attritionText),
                                Map.of("type", "mrkdwn", "text", "*Headcount (Funcionários):*\n" + headcountText),
                                Map.of("type", "mrkdwn", "text", "*Receita por Profissional:*\n" + revPerProfText)
                        )
                ));
            }

            // Bloco 4: Clientes & Retenção
            boolean hasClientData = topClientConc != null || top5ClientsConc != null || top10ClientsConc != null || netRetentionRate != null;
            if (hasClientData) {
                blocks.add(Map.of("type", "divider"));
                blocks.add(Map.of(
                        "type", "section",
                        "text", Map.of("type", "mrkdwn", "text", "*👥 Concentração de Clientes & Retenção*")
                ));
                blocks.add(Map.of(
                        "type", "section",
                        "fields", List.of(
                                Map.of("type", "mrkdwn", "text", "*Top 1 Cliente:*\n" + top1Text),
                                Map.of("type", "mrkdwn", "text", "*Top 5 Clientes:*\n" + top5Text),
                                Map.of("type", "mrkdwn", "text", "*Top 10 Clientes:*\n" + top10Text),
                                Map.of("type", "mrkdwn", "text", "*Net Retention Rate (NRR):*\n" + nrrText)
                        )
                ));
            }

            // Bloco 5: Exposição Setorial (Verticais)
            blocks.add(Map.of("type", "divider"));
            blocks.add(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text", "*🏢 Verticais de Indústria:*\n" + verticalsText.toString())
            ));

            // Bloco 6: Receita Geográfica
            blocks.add(Map.of(
                    "type", "section",
                    "text", Map.of("type", "mrkdwn", "text", "*🌍 Receita Geográfica:*\n" + geoText.toString())
            ));

            // Se ainda houver texto livre de exposição setorial (string antiga)
            if (report.getVerticalExposure() != null && !report.getVerticalExposure().isBlank()) {
                blocks.add(Map.of(
                        "type", "section",
                        "text", Map.of("type", "mrkdwn", "text", "*📌 Notas Setoriais:*\n" + report.getVerticalExposure())
                ));
            }

            // Contexto final
            blocks.add(Map.of(
                    "type", "context",
                    "elements", List.of(
                            Map.of("type", "mrkdwn", "text", "🤖 _Extração automática via Claude 3.5 Bedrock • Dados persistidos no PostgreSQL_")
                    )
            ));

            Map<String, Object> payload = Map.of("blocks", blocks);

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("🔔 Alerta executivo completo de {} ({}) enviado para o Slack com sucesso!", report.getCompany(), report.getTicker());

        } catch (Exception e) {
            log.error("Erro ao enviar notificação para o Slack: {}", e.getMessage(), e);
        }
    }

    private String resolveCurrencySymbol(String ticker) {
        if (ticker == null) return "R$";

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