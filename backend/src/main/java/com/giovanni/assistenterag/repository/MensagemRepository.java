package com.giovanni.assistenterag.repository;

import com.giovanni.assistenterag.model.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    List<Mensagem> findByConversaIdOrderByCriadoEmAsc(Long conversaId);
}