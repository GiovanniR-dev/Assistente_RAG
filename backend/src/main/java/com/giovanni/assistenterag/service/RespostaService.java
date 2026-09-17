package com.giovanni.assistenterag.service;

import com.giovanni.assistenterag.model.Conversa;
import com.giovanni.assistenterag.model.Mensagem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RespostaService {

    private static final int TRECHOS_DE_CONTEXTO = 8;
    private static final int MAXIMO_MENSAGENS_HISTORICO = 10;

    private final RestClient restClient;
    private final BuscaService buscaService;
    private final ConversaService conversaService;
    private final String modelo;

    public RespostaService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.modelo.chat}") String modelo,
            BuscaService buscaService,
            ConversaService conversaService) {

        this.modelo = modelo;
        this.buscaService = buscaService;
        this.conversaService = conversaService;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /** Pergunta avulsa, sem histórico. */

    /** Pergunta dentro de uma conversa, com histórico e persistência. */
    public String responderEmConversa(String pergunta, Conversa conversa) {
        Long usuarioiId=conversa.getUsuario().getId();
        List<Mensagem> historico = conversaService.listarMensagens(conversa.getId());
        String resposta = gerar(pergunta, historico, usuarioiId);

        conversaService.definirTituloSeVazio(conversa, pergunta);
        conversaService.salvarMensagem(conversa, ConversaService.PAPEL_USUARIO, pergunta);
        conversaService.salvarMensagem(conversa, ConversaService.PAPEL_ASSISTENTE, resposta);

        return resposta;
    }

    private String gerar(String pergunta, List<Mensagem> historico,long usuarioId) {
        List<BuscaService.TrechoRelevante> relevantes =
                buscaService.buscarTrechosRelevantes(pergunta,TRECHOS_DE_CONTEXTO,usuarioId);

        if (relevantes.isEmpty()) {
            return "Nenhum documento foi carregado ainda.";
        }

        String contexto = relevantes.stream()
                .map(r -> "[Fonte: %s]\n%s".formatted(r.nomeDocumento(), r.trecho().getConteudo()))
                .collect(Collectors.joining("\n\n---\n\n"));

        String instrucao = """
            Você é um assistente especializado em responder perguntas com base \
            em documentos fornecidos pelo usuário.

            Como trabalhar com os trechos abaixo:
            - Combine informações de trechos diferentes quando a resposta exigir isso.
            - Raciocine sobre os dados: comparar valores, calcular diferenças e tirar \
            conclusões a partir do que está escrito é esperado e desejável.
            - Se a informação aparecer em mais de uma fonte com valores divergentes, \
            aponte a divergência e cite os documentos envolvidos.
            - Não mencione os nomes dos arquivos na resposta.
            - Só diga que a informação não consta nos documentos quando ela realmente \
            não estiver presente nem puder ser derivada dos trechos.
            - Não use conhecimento próprio para preencher lacunas dos documentos.

            Responda em português, de forma direta e organizada.

            TRECHOS DOS DOCUMENTOS:
            %s
            """.formatted(contexto);

        List<MensagemChat> mensagens = new ArrayList<>();
        mensagens.add(new MensagemChat("system", instrucao));
        mensagens.addAll(converterHistorico(historico));
        mensagens.add(new MensagemChat("user", pergunta));

        RespostaChat resposta = restClient.post()
                .uri("/chat/completions")
                .body(new RequisicaoChat(modelo, mensagens))
                .retrieve()
                .body(RespostaChat.class);

        if (resposta == null || resposta.choices().isEmpty()) {
            throw new IllegalStateException("A API não retornou resposta.");
        }
        return resposta.choices().get(0).message().content();
    }

    private List<MensagemChat> converterHistorico(List<Mensagem> historico) {
        int inicio = Math.max(0, historico.size() - MAXIMO_MENSAGENS_HISTORICO);

        return historico.subList(inicio, historico.size()).stream()
                .map(m -> new MensagemChat(
                        ConversaService.PAPEL_USUARIO.equals(m.getPapel()) ? "user" : "assistant",
                        m.getConteudo()))
                .toList();
    }

    public record MensagemChat(String role, String content) {}

    public record RequisicaoChat(String model, List<MensagemChat> messages) {}

    public record RespostaChat(List<Escolha> choices) {
        public record Escolha(MensagemChat message) {}
    }
}