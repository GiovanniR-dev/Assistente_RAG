package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.config.UsuarioAtual;
import com.giovanni.assistenterag.model.Conversa;
import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.service.ConversaService;
import com.giovanni.assistenterag.service.RespostaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversas")
@RequiredArgsConstructor
public class ConversaController {

    private final ConversaService conversaService;
    private final RespostaService respostaService;
    private final UsuarioAtual usuarioAtual;

    @PostMapping
    public ResponseEntity<ConversaDto> criar() {
        Usuario usuario = usuarioAtual.get();
        Conversa conversa = conversaService.criar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConversaDto(conversa.getId(), conversa.getTitulo()));
    }

    @GetMapping
    public List<ConversaDto> listar() {
        return conversaService.listarPorUsuario(usuarioAtual.get().getId()).stream()
                .map(c -> new ConversaDto(c.getId(), c.getTitulo()))
                .toList();
    }

    @PostMapping("/{id}/mensagens")
    public MensagemDto enviar(@PathVariable Long id, @RequestBody PerguntaDto corpo) {
        Conversa conversa = conversaDoUsuario(id);
        String resposta = respostaService.responderEmConversa(corpo.pergunta(), conversa);
        return new MensagemDto(ConversaService.PAPEL_ASSISTENTE, resposta);
    }

    @GetMapping("/{id}/mensagens")
    public List<MensagemDto> historico(@PathVariable Long id) {
        Conversa conversa = conversaDoUsuario(id);
        return conversaService.listarMensagens(conversa.getId()).stream()
                .map(m -> new MensagemDto(m.getPapel(), m.getConteudo()))
                .toList();
    }

    private Conversa conversaDoUsuario(Long id) {
        Conversa conversa = conversaService.buscarPorId(id);

        if (!conversa.getUsuario().getId().equals(usuarioAtual.get().getId())) {
            throw new IllegalArgumentException("Conversa nao encontrada: " + id);
        }
        return conversa;
    }
}