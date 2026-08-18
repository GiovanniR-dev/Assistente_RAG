package com.giovanni.assistenterag.repository;

import com.giovanni.assistenterag.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    List<Conversa> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}