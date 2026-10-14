package com.mira.mira_api.domain.FinancialReports.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.domain.FinancialReports.dto.FinancialReportDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

@Service
public class FinancialDataExtractor {

    private static final Logger log = LoggerFactory.getLogger(FinancialDataExtractor.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        Você é um analista sênior de FP&A, Equity Research e Relações com Investidores focado na análise rigorosa dos relatórios contábeis de {company} ({ticker}).
        Analise cuidadosamente os demonstrativos financeiros (DRE / Income Statement, Fluxo de Caixa, Balanço Patrimonial e Destaques Financeiros/Operacionais/Apresentação de Resultados).
        O relatório pode estar redigido em PORTUGUÊS ou INGLÊS.

        INSTRUÇÕES CRÍTICAS DE EXTRAÇÃO:
        1. "netRevenue" (Receita Líquida):
           - Termos em inglês: "Net Revenue", "Total Net Revenue", "Revenue", "Revenues".
           - Termos em português: "Receita Líquida", "Receita Líquida Total", "Receita Operacional Líquida".
           - Priorize SEMPRE o resultado do trimestre corrente analisado. Retorne o valor numérico em MILHÕES.

        2. "revenueGrowth" (Crescimento YoY da Receita):
           - Taxa percentual de variação homóloga da receita líquida face ao mesmo trimestre do ano anterior (ex: 5.0, 3.2 ou -8.6).

        3. Rentabilidade e Margens (`profitability`):
           - "grossMargin": Margem Bruta ou Margem Bruta Ajustada (%).
           - "ebitdaMargin": Margem EBITDA ou % EBITDA Ajustado (%). Procure por "EBITDA", "Adjusted EBITDA", "Profit from Operations" ou "Adjusted Profit from Operations".
             *FALLBACK*: Se o EBITDA não estiver explícito, calcule-o somando o *Profit from Operations* com as despesas de *Depreciation & Amortization*. Divida pela receita líquida para obter a margem percentual. Se não houver dados, retorne 0.0.
           - "netMargin": Margem Líquida (%).
           - "fcfMargin": Margem de Fluxo de Caixa Livre (%).
           - "ruleOf40Score": Score da Regra dos 40.
           - "freeCashFlowConversion": Taxa de conversão de FCF.
           - "sgaToRevenue": Proporção de despesas SG&A em relação à Receita Líquida (%).

        4. Métricas Operacionais e de Equipe (`operationalMetrics`):
           - "utilizationRate" (Taxa de Utilização / Alocação): ATENÇÃO CRÍTICA: Procure por "Utilization Rate", "Billable Utilization", "Resource Utilization", "Consultant Utilization", "Employee Utilization", "Capacity Utilization", "Taxa de Utilização", "Taxa de Alocação" ou "Índice de Ocupação". Se omitido, retorne 0.0.
           - "attrition" (Rotatividade / Turnover): ATENÇÃO CRÍTICA: Procure obrigatoriamente pelos termos "Attrition", "Voluntary Attrition", "Turnover", "Rotatividade", "Taxa de Rotatividade" ou "Taxa de Saída" (geralmente presente nos slides de RH/Operacionais da Apresentação de Resultados). Extraia o valor percentual (ex: 12.5, 14.2). Se não encontrar de todo, retorne 0.0.
           - "employees": Número total de funcionários, colaboradores ou headcount (inteiro).
           - "revenuePerProfessional": Receita média por profissional, consultor ou funcionário.

        5. Clientes e Retenção (`clients`):
           - "topClientConcentration": Concentração de receita no maior cliente individual / Top 1 (%). Procure por "largest customer", "top client", "single customer". Se o relatório apenas mencionar o Top 5 ou Top 10 e omitir o Top 1, analise se há menção a "no customer represents more than X%" e use esse limite, caso contrário retorne 0.0.
           - "top5ClientsConcentration": Concentração nos 5 maiores clientes / Top 5 (%). Procure por "top 5 clients", "five largest customers".
           - "top10ClientsConcentration": Concentração nos 10 maiores clientes / Top 10 (%). Procure por "top 10 clients", "ten largest customers".
           - "netRetentionRate": Net Retention Rate (NRR) ou taxa de retenção líquida de receita (%).

        6. "industryVerticals" (Distribuição por Verticais de Indústria):
           - Extraia a lista dinâmica de segmentos, indústrias ou verticais apresentados no relatório ou em gráficos (ex: Media and Entertainment, Banking/Financial Services, Retail, Tech, Healthcare, etc.).
           - Para cada vertical encontrada:
             - "name": O nome exato da vertical.
             - "revenue": Se a apresentação contiver apenas a porcentagem (%) da vertical em um gráfico (ex: 21%, 20%, 18%), CALCULE O VALOR EM MILHÕES multiplicando essa porcentagem pela Receita Líquida Total (`netRevenue`). Se o valor em dinheiro já estiver explícito, use-o diretamente.
             - "growth": A taxa de crescimento associada (se disponível, senão 0.0).
           - Se não houver dados de verticais, retorne uma lista vazia [].

        7. "geographicRevenue" (Distribuição Geográfica da Receita):
           - Extraia a lista dinâmica de regiões, países ou mercados geográficos apresentados (ex: North America, LATAM, Europe, New Markets, etc.).
           - Para cada região encontrada:
             - "name": O nome exato da região ou país.
             - "percentage": O valor percentual que a região representa (ex: 53.0, 21.0, 5.0).
             - "revenue": Se estiver apenas em porcentagem, CALCULE O VALOR EM MILHÕES (porcentagem * Receita Líquida Total). Se o valor em dinheiro já estiver explícito, use-o diretamente.
           - Se não houver dados geográficos segmentados, retorne uma lista vazia [].

        8. Exposição de Mercado:
           - "verticalExposure": Resumo ou distribuição descritiva adicional da receita por verticais de indústria (se houver).

        ESTRUTURA JSON EXIGIDA (Mantenha as chaves inalteradas):
        {
          "revenues": {
            "netRevenue": 0.0,
            "revenueGrowth": 0.0,
            "saasRevenue": 0.0,
            "recurringRevenue": 0.0,
            "cloudRevenue": 0.0
          },
          "profitability": {
            "grossMargin": 0.0,
            "ebitdaMargin": 0.0,
            "netMargin": 0.0,
            "fcfMargin": 0.0,
            "ruleOf40Score": 0.0,
            "freeCashFlowConversion": 0.0,
            "sgaToRevenue": 0.0
          },
          "valuation": {
            "freeCashFlow": 0.0,
            "cashAndEquivalents": 0.0,
            "grossDebt": 0.0,
            "netDebt": 0.0,
            "netDebtEbitdaRatio": 0.0
          },
          "operationalMetrics": {
            "annualRecurringRevenueArr": 0.0,
            "arrNetAddition": 0.0,
            "netRetentionRateNrr": 0.0,
            "saasRenewalRate": 0.0,
            "saasGrossChurnRate": 0.0,
            "subscriptionRevenueGrowth": 0.0,
            "utilizationRate": 0.0,
            "attrition": 0.0,
            "employees": 0,
            "revenuePerProfessional": 0.0
          },
          "clients": {
            "topClientConcentration": 0.0,
            "top5ClientsConcentration": 0.0,
            "top10ClientsConcentration": 0.0,
            "netRetentionRate": 0.0
          },
          "industryVerticals": [
            {
              "name": "Nome da Vertical",
              "revenue": 0.0,
              "growth": 0.0
            }
          ],
          "geographicRevenue": [
            {
              "name": "Nome da Região/País",
              "revenue": 0.0,
              "percentage": 0.0
            }
          ],
          "verticalExposure": ""
        }

        REGRAS DE FORMATAÇÃO:
        - Utilize sempre números decimais com ponto (.) como separador.
        - Caso uma métrica não esteja presente, atribua 0.0 (ou 0 para inteiros / "" para texto).
        - Não inclua símbolos monetários ("R$", "$") nem de porcentagem ("%").
        - Devolva estritamente o JSON válido sem anotações adicionais.
    """;

    public FinancialDataExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public FinancialReportDTO extractMetricsFromImages(List<byte[]> pageImages, String companyName, String ticker) {
        List<byte[]> targetedPages = pageImages.size() > 20 ? pageImages.subList(0, 20) : pageImages;
        log.info("Enviando as {} principais páginas estratégicas de {} ({}) para a IA multimodal...",
                targetedPages.size(), companyName, ticker);

        List<Media> mediaList = targetedPages.stream()
                .map(bytes -> new Media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(bytes)))
                .toList();

        String systemPrompt = buildSystemPrompt(companyName, ticker);

        String rawResponse = chatClient.prompt()
                .options(ChatOptions.builder()
                        .maxTokens(4000)
                        .temperature(0.0)
                        .build())
                .system(systemPrompt)
                .user(u -> u.text("Extraia com rigor as métricas contábeis e operacionais das páginas anexas:")
                        .media(mediaList.toArray(new Media[0])))
                .call()
                .content();

        return parseJsonToDTO(rawResponse);
    }

    public FinancialReportDTO extractMetrics(String fullPdfText, String companyName, String ticker) {
        log.info("Extraindo métricas via Texto no Bedrock para {} ({})...", companyName, ticker);

        String systemPrompt = buildSystemPrompt(companyName, ticker);

        String rawResponse = chatClient.prompt()
                .options(ChatOptions.builder()
                        .maxTokens(4000)
                        .temperature(0.0)
                        .build())
                .system(systemPrompt)
                .user(u -> u.text("Extraia os dados financeiros estruturados do seguinte texto do relatório:\n\n" + fullPdfText))
                .call()
                .content();

        return parseJsonToDTO(rawResponse);
    }

    private String buildSystemPrompt(String companyName, String ticker) {
        return SYSTEM_PROMPT_TEMPLATE
                .replace("{company}", companyName)
                .replace("{ticker}", ticker);
    }

    private FinancialReportDTO parseJsonToDTO(String rawResponse) {
        try {
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new IllegalArgumentException("Resposta vazia recebida do modelo.");
            }

            String cleanedJson = rawResponse
                    .replaceAll("(?i)```json", "")
                    .replaceAll("```", "")
                    .trim();

            int firstBrace = cleanedJson.indexOf('{');
            int lastBrace = cleanedJson.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleanedJson = cleanedJson.substring(firstBrace, lastBrace + 1);
            }

            log.info("Resposta formatada do Bedrock:\n{}", cleanedJson);
            return objectMapper.readValue(cleanedJson, FinancialReportDTO.class);
        } catch (Exception e) {
            log.error("Error parsing JSON from Bedrock: {}", rawResponse, e);
            throw new RuntimeException("Falha ao converter dados da IA para FinancialReportDTO", e);
        }
    }
}