package com.giovanni.assistenterag.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor


public class Documento {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name="nome_arquivo",nullable = false,length = 255)
    protected String nomeArquivo;

    @Column(length = 50)
    private String tipo;

    @Column(name="criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;
}
