package com.mira.mira_api.domain.company.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "TB_companies")
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // ex: "TOTVS"

    @Column(nullable = false, unique = true)
    private String ticker; // ex: "TOTS3"

    @Column(name = "currency", length = 10)
    private String currency = "BRL";

    @Column(nullable = false)
    private String sector; // ex: "Tecnologia / Software de Gestão"

    @Column(name = "provider_type", nullable = false)
    private String providerType; // ex: "MZIQ", "GENERIC_URL", "SCRAPING"

    @Column(name = "api_url")
    private String apiUrl; // ex: endpoint do MZIQ para busca de documentos

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
