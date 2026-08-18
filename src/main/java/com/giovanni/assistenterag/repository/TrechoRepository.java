package com.giovanni.assistenterag.repository;

import com.giovanni.assistenterag.model.Trecho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrechoRepository extends JpaRepository<Trecho, Long> {
    List<Trecho> findByDocumentoId(Long DocumentoId);
}
