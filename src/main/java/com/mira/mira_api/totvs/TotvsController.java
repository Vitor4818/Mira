package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.dto.DocumentMeta;
import com.mira.mira_api.totvs.model.ReportEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/totvs")
public class TotvsController {

    private final TotvsReportService service;
    private final TotvsReportSyncService syncService;

    // Injeção de ambos os serviços via construtor
    public TotvsController(TotvsReportService service, TotvsReportSyncService syncService) {
        this.service = service;
        this.syncService = syncService;
    }

    @GetMapping("/reports")
    public List<DocumentMeta> reports() {
        return service.findReports();
    }

    @PostMapping("/sync")
    public List<ReportEntity> sync(@RequestParam(defaultValue = "2025") String year) {
        return syncService.syncReports(year);
    }
}