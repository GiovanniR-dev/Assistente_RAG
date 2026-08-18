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

    public Documento processarUpload(MultipartFile arquivo, Usuario usuario) throws IOException {
        String textoCompleto = extrairTexto(arquivo);
        Documento documento = new Documento();
        documento.setUsuario(usuario);
        documento.setNomeArquivo(arquivo.getOriginalFilename());
        documento.setTipo("pdf");
        documento = documentoRepository.save(documento);

        List<String> pedacos= dividirEmTrechos(textoCompleto);

        int ordem=0;
        for(String pedaco:pedacos){
            Trecho trecho= new Trecho();
            trecho.setDocumento(documento);
            trecho.setConteudo(pedaco);
            trecho.setOrdem(ordem++);
            trecho.setEmbedding("[]");
            trechoRepository.save(trecho);
        }
        return documento;
    }

    private String extrairTexto(MultipartFile arquivo) throws IOException{
        try(PDDocument pdf= Loader.loadPDF(arquivo.getBytes())){
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdf);
        }
    }

    private List<String> dividirEmTrechos(String texto){
        List<String> trechos=new ArrayList<>();
        String[] paragrafos=texto.split("\\n\\s*\\n");

        for(String paragrafo:paragrafos){
            String limpo=paragrafo.trim();
            if(!limpo.isEmpty()){
                trechos.add(limpo);
            }
        }
        return trechos;
    }


}
