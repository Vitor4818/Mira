package com.mira.mira_api.totvs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mira.mira_api.totvs.dto.FinancialReportDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions; // Import genérico
import org.springframework.stereotype.Service;

@Service
public class FinancialDataExtractor {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FinancialDataExtractor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public FinancialReportDTO extractMetrics(String pdfText) {
        String rawResponse = chatClient
                .prompt()
                .options(ChatOptions.builder()
                        .maxTokens(3000)      // Define o limite de tokens para 3000
                        .temperature(0.0)     // Temperatura zero para resposta direta/fiel
                        .build())
                .system("""
                    Você é um analista financeiro sênior especialista em relatórios de RI brasileiros.
                    O texto fornecido pertence a um relatório trimestral de Resultados (Release de Resultados / DRE) em PORTUGUÊS.
                    
                    REGRAS DE CONCISÃO E FORMATO (EXTREMAMENTE IMPORTANTES):
                    1. Retorne EXCLUSIVAMENTE o JSON bruto correspondente ao schema abaixo.
                    2. NUNCA inclua marcações de código como ```json ou ```.
                    3. NÃO inclua nenhum texto explicativo, saudação ou rodapé.
                    4. Seja direto nos valores numéricos para garantir que o JSON feche todas as chaves '}' perfeitamente.

                    ESTRUTURA ESPERADA:
                    {
                      "revenue": {
                        "revenue": 1555.3,
                        "revenueGrowth": 17.8,
                        "industryVerticals": [
                          { "name": "Gestão", "revenue": 1393.4, "growth": 18.1 },
                          { "name": "RD Station", "revenue": 161.9, "growth": 15.4 }
                        ]
                      },
                      "profitability": {
                        "grossMargin": 73.0,
                        "ebitdaMargin": 26.0,
                        "sgaToRevenue": null
                      },
                      "clients": {
                        "netRetentionRate": 98.6,
                        "topClientsConcentration": null
                      },
                      "operational": {
                        "employees": null,
                        "attrition": null,
                        "revenuePerProfessional": null,
                        "utilizationRate": null
                      },
                      "valuation": {
                        "freeCashFlow": 280.5
                      }
                    }

                    REGRAS DE DADOS:
                    - Extraia o valor numérico puro (ex: 73.2).
                    - Se a métrica não estiver no texto, coloque null.
                """)
                .user(pdfText)
                .call()
                .content();

        // Higieniza formatações residuais Markdown
        String cleanedJson = rawResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("```", "")
                .trim();

        try {
            return objectMapper.readValue(cleanedJson, FinancialReportDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao realizar o parse do JSON do Bedrock: " + e.getMessage() + "\nJSON Recebido:\n" + cleanedJson, e);
        }
    }
}