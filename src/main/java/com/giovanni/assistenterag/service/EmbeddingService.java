package com.giovanni.assistenterag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final String MODELO = "text-embedding-3-small";

    private final RestClient restClient;

    public EmbeddingService(@Value("${openai.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public List<Double> gerarEmbedding(String texto) {
        RespostaEmbedding resposta = restClient.post()
                .uri("/embeddings")
                .body(new RequisicaoEmbedding(MODELO, texto))
                .retrieve()
                .body(RespostaEmbedding.class);

        if (resposta == null || resposta.data().isEmpty()) {
            throw new IllegalStateException("A API não retornou embedding para o texto enviado.");
        }
        return resposta.data().get(0).embedding();
    }

    public String paraJson(List<Double> vetor) {
        return vetor.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    public List<Double> deJson(String json) {
        String limpo = json.replace("[", "").replace("]", "").trim();
        if (limpo.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(limpo.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .toList();
    }

    public record RequisicaoEmbedding(String model, String input) {}

    public record RespostaEmbedding(List<Dado> data) {
        public record Dado(List<Double> embedding) {}
    }
}