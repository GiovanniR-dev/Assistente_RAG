package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.config.UsuarioAtual;
import com.giovanni.assistenterag.model.Documento;
import com.giovanni.assistenterag.model.Usuario;
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
    private final UsuarioAtual usuarioAtual;

    @PostMapping("/upload")
    public ResponseEntity<RespostaUpload> upload(@RequestParam("arquivo") MultipartFile arquivo)
            throws IOException {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuario = usuarioAtual.get();

        Documento documento = documentoService.processarUpload(arquivo, usuario);

        RespostaUpload resposta = new RespostaUpload(
                documento.getId(),
                documento.getNomeArquivo()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public List<RespostaUpload> listar() {
        return documentoService.listarPorUsuario(usuarioAtual.get().getId()).stream()
                .map(doc -> new RespostaUpload(doc.getId(), doc.getNomeArquivo()))
                .toList();
    }

    public record RespostaUpload(Long id, String nomeArquivo) {}
}