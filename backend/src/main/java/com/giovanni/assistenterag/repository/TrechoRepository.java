package com.giovanni.assistenterag.repository;

import com.giovanni.assistenterag.model.Trecho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrechoRepository extends JpaRepository<Trecho, Long> {
    List<Trecho> findByDocumentoId(Long DocumentoId);

    @Query("SELECT t FROM Trecho t WHERE t.documento.usuario.id = :usuarioId")
    List<Trecho> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
