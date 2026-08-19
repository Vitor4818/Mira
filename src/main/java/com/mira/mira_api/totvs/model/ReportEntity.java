package com.mira.mira_api.totvs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String fileTitle;

    private Integer fileQuarter;
    private Integer fileYear;

    @Column(nullable = false, unique = true, length = 1000)
    private String downloadLinkId;

    @Column(length = 1000)
    private String fileUrl;

    private String localFilePath;

    private LocalDateTime downloadedAt;

    public ReportEntity() {}

    public ReportEntity(String companyName, String fileTitle, Integer fileQuarter, Integer fileYear,
                        String downloadLinkId, String fileUrl, String localFilePath) {
        this.companyName = companyName;
        this.fileTitle = fileTitle;
        this.fileQuarter = fileQuarter;
        this.fileYear = fileYear;
        this.downloadLinkId = downloadLinkId;
        this.fileUrl = fileUrl;
        this.localFilePath = localFilePath;
        this.downloadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getFileTitle() { return fileTitle; }
    public Integer getFileQuarter() { return fileQuarter; }
    public Integer getFileYear() { return fileYear; }
    public String getDownloadLinkId() { return downloadLinkId; }
    public String getFileUrl() { return fileUrl; }
    public String getLocalFilePath() { return localFilePath; }
    public LocalDateTime getDownloadedAt() { return downloadedAt; }
}