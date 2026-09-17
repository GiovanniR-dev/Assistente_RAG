package com.giovanni.assistenterag.service;


import com.giovanni.assistenterag.model.Trecho;
import com.giovanni.assistenterag.repository.TrechoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BuscaService {



    private final TrechoRepository trechoRepository;
    private final EmbeddingService embeddingService;
    @Transactional(readOnly = true)
    public List<TrechoRelevante> buscarTrechosRelevantes(String pergunta, int quantidade, Long usuarioId) {
        List<Double> embeddingPergunta = embeddingService.gerarEmbedding(pergunta);

        List<TrechoRelevante> ordenados = trechoRepository.findByUsuarioId(usuarioId).stream()
                .map(trecho -> new TrechoRelevante(
                        trecho,
                        trecho.getDocumento().getNomeArquivo(),
                        embeddingService.similaridadeCosseno(
                                embeddingPergunta,
                                embeddingService.deJson(trecho.getEmbedding())
                        )
                ))
                .sorted(Comparator.comparingDouble(TrechoRelevante::similaridade).reversed())
                .toList();

                return selecionarComDiversidade(ordenados,quantidade);
    }

    private List<TrechoRelevante> selecionarComDiversidade(
            List<TrechoRelevante> ordenados,int quantidade){
        int maximoPorDocumento=Math.max(2,quantidade/3);
        Map<String,Integer>usados=new HashMap<>();
        List<TrechoRelevante> selecionados=new ArrayList<>();

        for (TrechoRelevante candidato:ordenados){
            if (selecionados.size()>= quantidade) break;

            int jaUsados=usados.getOrDefault(candidato.nomeDocumento(),0);
            if (jaUsados<maximoPorDocumento){
                selecionados.add(candidato);
                usados.put(candidato.nomeDocumento(),jaUsados+1);
            }
        }

        for (TrechoRelevante candidato:ordenados){
            if (selecionados.size()>=quantidade)break;
            if (!selecionados.contains(candidato)){
                selecionados.add(candidato);
            }
        }
        return selecionados;

    }
    public record TrechoRelevante(Trecho trecho, String nomeDocumento, double similaridade) {}
}
