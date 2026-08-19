package com.giovanni.assistenterag.service;


import com.giovanni.assistenterag.model.Trecho;
import com.giovanni.assistenterag.repository.TrechoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuscaService {

    private final TrechoRepository trechoRepository;
    private final EmbeddingService embeddingService;

    public List<TrechoRelevante> buscartrechoRelevante(String pergunta, int quantidade){
        List<Double> embeddingPergunta=embeddingService.gerarEmbedding(pergunta);
        return trechoRepository.findAll().stream()
                .map(trecho ->new TrechoRelevante(
                        trecho,
                        embeddingService.similaridadeCosseno(
                                embeddingPergunta,
                                embeddingService.deJson(trecho.getEmbedding())
                        )
                ))
                .sorted(Comparator.comparingDouble(TrechoRelevante::similaridade).reversed())
                .limit(quantidade)
                .toList();
    }
    public record TrechoRelevante(Trecho trecho, double similaridade){}
}
