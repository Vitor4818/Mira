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
        Você é um analista sénior de FP&A, Equity Research e Relações com Investidores focado na análise rigorosa dos relatórios contábeis de {company} ({ticker}).
        Analise cuidadosamente os demonstrativos financeiros (DRE / Income Statement / Statement of Operations, Fluxo de Caixa / Cash Flows, Balanço / Balance Sheet e Destaques Financeiros).
        O relatório pode estar redigido em PORTUGUÊS ou INGLÊS.

        INSTRUÇÕES CRÍTICAS DE EXTRAÇÃO:
        1. "netRevenue" (Receita Líquida):
           - Termos em inglês: "Net Revenue", "Total Net Revenue", "Revenue", "Revenues".
           - Termos em português: "Receita Líquida", "Receita Líquida Total", "Receita Operacional Líquida".
           - Priorize SEMPRE o resultado do trimestre corrente analisado (ex: "Three Months Ended", "3M", "Trimestre", "1T/2T/3T/4T", "Q1/Q2/Q3/Q4"). Não utilize o acumulado do ano (6M/9M/FY/12M) a menos que o período analisado seja exclusivamente esse.
           - ESCALA E UNIDADE: Retorne SEMPRE o valor numérico em MILHÕES.
             * Se o relatório estiver em BILIÕES (ex: Accenture reportando $18.7 billion ou $18,718 million), converta multiplicando por 1000 para manter em milhões (ex: 18718.1).
             * Se estiver em milhares (in thousands, ex: 117,200), converta dividindo por 1000 (ex: 117.2).

        2. "revenueGrowth" (Crescimento YoY da Receita):
           - Taxa percentual de variação homóloga da receita líquida face ao mesmo trimestre do ano anterior (YoY / Year-over-Year).
           - Termos: "YoY Growth", "Net Revenue YoY", "Constant Currency YoY Growth", "Crescimento YoY", "Crescimento de Receita (YoY)".
           - Extraia apenas o número decimal com sinal adequado (ex: 5.0, 3.2 ou -8.6).

        3. "grossMargin" (Margem Bruta):
           - Termos: "Gross Margin", "Adjusted Gross Margin", "Margem Bruta", "Margem Bruta Ajustada".
           - Se não estiver explícita, calcule: (Gross Profit / Net Revenue) * 100.
           - Retorne em percentagem (ex: 32.8 ou 72.0).

        4. "ebitdaMargin" (Margem EBITDA ou Margem Operacional):
           - Termos de busca: "Adjusted EBITDA Margin", "EBITDA Margin", "Margem EBITDA", "Margem EBITDA Ajustada", "Adjusted Operating Margin", "Operating Margin", "Operating Income Margin", "Adjusted PBT Margin", "Margem Operacional".
           - REGRA FUNDAMENTAL: Empresas que seguem US GAAP ou IFRS puro (como Accenture e Endava) muitas vezes NÃO reportam a métrica EBITDA, reportando prioritariamente a Margem Operacional ("Operating Margin"). Nesses casos, extraia a Operating Margin como valor representativo desta métrica.
           - Retorne em percentagem (ex: 15.2 ou 16.8).

        5. "freeCashFlow" (Fluxo de Caixa Livre) e "fcfMargin":
           - Procure expressamente por: "Free Cash Flow", "FCF", "Fluxo de Caixa Livre".
           - Se não houver linha explícita de Free Cash Flow, calcule: "Cash flow from operating activities" (Caixa Operacional) menos "Capital Expenditures / Purchase of property, plant and equipment / Purchase of fixed assets" (Capex).
           - Retorne o valor em MILHÕES (ex: 3600.0, 20.1 ou -3.1).
           - "fcfMargin": margem percentual de fluxo de caixa livre caso conste do documento.

        6. Métricas Operacionais e de Balanço:
           - "annualRecurringRevenueArr": Saldo ou adição de ARR em milhões, se divulgado.
           - "subscriptionRevenueGrowth": Crescimento de receita recorrente/SaaS YoY.
           - "cashAndEquivalents": Caixa e equivalentes de caixa em milhões.
           - "grossDebt": Dívida Bruta em milhões.
           - "netDebt": Dívida Líquida em milhões.

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
            "freeCashFlowConversion": 0.0
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
            "subscriptionRevenueGrowth": 0.0
          },
          "clients": {
            "totalClients": 0,
            "corporateClients": 0,
            "fintechClients": 0,
            "saasClients": 0
          },
          "industryVerticals": {
            "verticalManagement": 0.0,
            "verticalBusinessPerformance": 0.0,
            "verticalTechfin": 0.0,
            "crossSellingPenetration": 0.0
          }
        }

        REGRAS DE FORMATAÇÃO:
        - Utilize sempre números decimais com ponto (.) como separador.
        - Caso uma métrica não esteja presente no relatório, atribua 0.0 ou 0.
        - Não inclua símbolos monetários ("R$", "$", "£") nem de percentagem ("%").
        - Devolva estritamente o JSON válido sem anotações adicionais.
    """;

    public FinancialDataExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public FinancialReportDTO extractMetricsFromImages(List<byte[]> pageImages, String companyName, String ticker) {
        List<byte[]> targetedPages = pageImages.size() > 8 ? pageImages.subList(0, 8) : pageImages;
        log.info("🖼️ Enviando as {} principais páginas estratégicas de {} ({}) para a IA multimodal...",
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
        log.info("🧠 Extraindo métricas via Texto no Bedrock para {} ({})...", companyName, ticker);

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

            log.info("📄 Resposta formatada do Bedrock:\n{}", cleanedJson);
            return objectMapper.readValue(cleanedJson, FinancialReportDTO.class);
        } catch (Exception e) {
            log.error("Erro ao fazer parse do JSON do Bedrock: {}", rawResponse, e);
            throw new RuntimeException("Falha ao converter dados da IA para FinancialReportDTO", e);
        }
    }
}