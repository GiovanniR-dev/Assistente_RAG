package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.model.Documento;
import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.repository.UsuarioRepository;
import com.giovanni.assistenterag.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/upload")
    public ResponseEntity<RespostaUpload> upload(@RequestParam("arquivo") MultipartFile arquivo)
            throws IOException {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Temporário: usa o primeiro usuário do banco até termos autenticação
        Usuario usuario = usuarioRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum usuário cadastrado no banco."));

        Documento documento = documentoService.processarUpload(arquivo, usuario);

        RespostaUpload resposta = new RespostaUpload(
                documento.getId(),
                documento.getNomeArquivo()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public List<RespostaUpload> listar() {
        return documentoService.listarTodos().stream()
                .map(doc -> new RespostaUpload(doc.getId(), doc.getNomeArquivo()))
                .toList();
    }

    public record RespostaUpload(Long id, String nomeArquivo) {}
}