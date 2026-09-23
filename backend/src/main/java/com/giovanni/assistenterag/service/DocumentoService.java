package com.giovanni.assistenterag.service;

import com.giovanni.assistenterag.model.Documento;
import com.giovanni.assistenterag.model.Trecho;
import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.repository.DocumentoRepository;
import com.giovanni.assistenterag.repository.TrechoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final TrechoRepository trechoRepository;
    private final EmbeddingService embeddingService;

    public Documento processarUpload(MultipartFile arquivo, Usuario usuario) throws IOException {
        String nomeArquivo = arquivo.getOriginalFilename();

        List<Documento> existentes = documentoRepository.findByUsuarioId(usuario.getId());

        if (existentes.size() >= MAXIMO_DOCUMENTOS_POR_USUARIO) {
            throw new IllegalArgumentException(
                    "Limite de %d documentos por usuario atingido."
                            .formatted(MAXIMO_DOCUMENTOS_POR_USUARIO));
        }

        boolean jaExiste = existentes.stream()
                .anyMatch(d -> d.getNomeArquivo().equals(nomeArquivo));

        if (jaExiste) {
            throw new IllegalArgumentException(
                    "Ja existe um documento com esse nome: " + nomeArquivo);
        }

        String textoCompleto = extrairTexto(arquivo);

        if (textoCompleto == null || textoCompleto.isBlank()) {
            throw new IllegalArgumentException(
                    "Nao foi possivel extrair texto do arquivo. "
                            + "PDFs formados apenas por imagens nao sao suportados.");
        }

        List<String> pedacos = dividirEmTrechos(textoCompleto);

        if (pedacos.size() > MAXIMO_TRECHOS_POR_DOCUMENTO) {
            throw new IllegalArgumentException(
                    "Documento muito grande: %d trechos (maximo %d). Envie um arquivo menor."
                            .formatted(pedacos.size(), MAXIMO_TRECHOS_POR_DOCUMENTO));
        }

        Documento documento = new Documento();
        documento.setUsuario(usuario);
        documento.setNomeArquivo(nomeArquivo);
        documento.setTipo("pdf");
        documento = documentoRepository.save(documento);

        int ordem = 0;
        for (String pedaco : pedacos) {
            Trecho trecho = new Trecho();
            trecho.setDocumento(documento);
            trecho.setConteudo(pedaco);
            trecho.setOrdem(ordem++);

            List<Double> vetor = embeddingService.gerarEmbedding(pedaco);
            trecho.setEmbedding(embeddingService.paraJson(vetor));

            trechoRepository.save(trecho);
        }

        return documento;
    }

    public List<Documento> listarPorUsuario(Long usuarioId) {
        return documentoRepository.findByUsuarioId(usuarioId);
    }
    private String extrairTexto(MultipartFile arquivo) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(arquivo.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdf);
        }
    }

    private static final int TAMANHO_TRECHO = 1000;
    private static final int SOBREPOSICAO = 200;
    private static final int MAXIMO_TRECHOS_POR_DOCUMENTO=150;
    private static final int MAXIMO_DOCUMENTOS_POR_USUARIO = 10;

    private List<String> dividirEmTrechos(String texto) {
        List<String> trechos = new ArrayList<>();
        String limpo = texto.replaceAll("\\s+", " ").trim();

        if (limpo.isEmpty()) {
            return trechos;
        }

        int inicio = 0;
        while (inicio < limpo.length()) {
            int fim = Math.min(inicio + TAMANHO_TRECHO, limpo.length());

            if (fim < limpo.length()) {
                int ultimoPonto = limpo.lastIndexOf(". ", fim);
                if (ultimoPonto > inicio + TAMANHO_TRECHO / 2) {
                    fim = ultimoPonto + 1;
                }
            }

            trechos.add(limpo.substring(inicio, fim).trim());

            if (fim >= limpo.length()) {
                break;
            }
            inicio = fim - SOBREPOSICAO;
        }
        return trechos;
    }
}