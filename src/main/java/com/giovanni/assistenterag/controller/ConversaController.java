package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.model.Conversa;
import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<ConversaDto> criar(){
        Usuario usuario=usuarioAtual();
        Conversa conversa=conversaService.criar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConversaDto(conversa.getId(), conversa.getTitulo()));
    }

    @GetMapping
    public List<ConversaDto> listar() {
        return conversaService.listarPorUsuario(usuarioAtual().getId()).stream()
                .map(c -> new ConversaDto(c.getId(), c.getTitulo()))
                .toList();
    }

    @PostMapping("/{id}/mensagens")
    public MensagemDto enviar(@PathVariable Long id, @RequestBody PerguntaDto corpo){
        Conversa conversa=conversaService.buscarPorId(id);
        String resposta=respostaService.responderEmConversa(corpo.pergunta(), conversa);
        return new MensagemDto(ConversaService.PAPEL_ASSISTENTE, resposta);

    }

    @GetMapping("/{id}/mensagens")
    public List<MensagemDto> historico(@PathVariable Long id) {
        return conversaService.listarMensagens(id).stream()
                .map(m -> new MensagemDto(m.getPapel(), m.getConteudo()))
                .toList();
    }

    private Usuario usuarioAtual() {
        return usuarioRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum usuario cadastrado."));
    }
}


