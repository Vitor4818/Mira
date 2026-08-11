package com.mira.mira_api.totvs;

import com.mira.mira_api.totvs.dto.DocumentMeta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/totvs")
public class TotvsController {

    private final TotvsReportService service;

    public TotvsController(TotvsReportService service) {
        this.service = service;
    }

    @GetMapping("/reports")
    public List<DocumentMeta> reports() {
        return service.findReports();
    }
}