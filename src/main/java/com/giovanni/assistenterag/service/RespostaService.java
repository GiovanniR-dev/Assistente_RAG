package com.giovanni.assistenterag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RespostaService {

    private static final int TRECHOS_DE_CONTEXTO = 3;

    private final RestClient restClient;
    private final BuscaService buscaService;
    private final String modelo;

    public RespostaService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.modelo.chat}") String modelo,
            BuscaService buscaService) {

        this.modelo = modelo;
        this.buscaService = buscaService;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String responder(String pergunta) {
        List<BuscaService.TrechoRelevante> relevantes =
                buscaService.buscartrechoRelevante(pergunta, TRECHOS_DE_CONTEXTO);

        if (relevantes.isEmpty()) {
            return "Nenhum documento foi carregado ainda.";
        }

        String contexto = relevantes.stream()
                .map(r -> r.trecho().getConteudo())
                .collect(Collectors.joining("\n\n---\n\n"));

        String instrucao = """
                Você é um assistente que responde perguntas com base exclusivamente \
                nos trechos de documento fornecidos abaixo.

                Regras:
                - Use apenas as informações dos trechos. Não invente nada.
                - Se os trechos não contiverem a resposta, diga que a informação \
                não está no documento.
                - Responda em português, de forma direta.

                TRECHOS DO DOCUMENTO:
                %s
                """.formatted(contexto);

        RespostaChat resposta = restClient.post()
                .uri("/chat/completions")
                .body(new RequisicaoChat(modelo, List.of(
                        new MensagemChat("system", instrucao),
                        new MensagemChat("user", pergunta)
                )))
                .retrieve()
                .body(RespostaChat.class);

        if (resposta == null || resposta.choices().isEmpty()) {
            throw new IllegalStateException("A API não retornou resposta.");
        }
        return resposta.choices().get(0).message().content();
    }

    public record MensagemChat(String role, String content) {}

    public record RequisicaoChat(String model, List<MensagemChat> messages) {}

    public record RespostaChat(List<Escolha> choices) {
        public record Escolha(MensagemChat message) {}
    }
}