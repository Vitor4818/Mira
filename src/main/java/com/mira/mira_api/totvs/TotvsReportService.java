package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.dto.DocumentMeta;
import com.mira.mira_api.totvs.dto.totvsRequest;
import com.mira.mira_api.totvs.dto.totvsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TotvsReportService {

    private final TotvsClient client;

    public TotvsReportService(TotvsClient client) {
        this.client = client;
    }

    public List<DocumentMeta> findReports() {
        var request = new totvsRequest(
                List.of("central_de_resultados_release_de_resultados"),
                "pt_BR",
                true,
                "2026"
        );

        totvsResponse response = client.getReports(request);

        if (response == null || response.data() == null || response.data().documentMetas() == null) {
            return List.of();
        }

        return response.data().documentMetas();
    }
}