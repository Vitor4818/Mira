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
    private String name;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(name = "currency", length = 10)
    private String currency = "BRL";

    @Column(nullable = false)
    private String sector;

    @Column(name = "provider_type", nullable = false)
    private String providerType;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
