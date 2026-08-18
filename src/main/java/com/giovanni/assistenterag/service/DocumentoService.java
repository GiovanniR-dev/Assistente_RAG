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
        String textoCompleto = extrairTexto(arquivo);

        Documento documento = new Documento();
        documento.setUsuario(usuario);
        documento.setNomeArquivo(arquivo.getOriginalFilename());
        documento.setTipo("pdf");
        documento = documentoRepository.save(documento);

        List<String> pedacos = dividirEmTrechos(textoCompleto);

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

    public List<Documento> listarTodos() {
        return documentoRepository.findAll();
    }

    private String extrairTexto(MultipartFile arquivo) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(arquivo.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdf);
        }
    }

    private static final int TAMANHO_TRECHO = 1000;
    private static final int SOBREPOSICAO = 200;

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