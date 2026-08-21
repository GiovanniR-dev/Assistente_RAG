package com.giovanni.assistenterag.service;

import com.giovanni.assistenterag.model.Conversa;
import com.giovanni.assistenterag.model.Mensagem;
import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.repository.ConversaRepository;
import com.giovanni.assistenterag.repository.MensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversaService {

    public static final String PAPEL_USUARIO="usuario";
    public static final String PAPEL_ASSISTENTE="assistente";

    private static final int TAMANHO_MAXIMO_TITULO=60;

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;

    public Conversa criar(Usuario usuario){
        Conversa conversa=new Conversa();
        conversa.setUsuario(usuario);
        return  conversaRepository.save(conversa);
    }

    public Conversa buscarPorId(Long id){
        return conversaRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Conversa nao encontrada: " + id));
    }


    public List<Conversa> listarPorUsuario(Long usuarioId) {
        return conversaRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);
    }

    public List<Mensagem> listarMensagens(Long conversaId) {
        return mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId);
    }

    public void salvarMensagem(Conversa conversa, String papel, String conteudo){
        Mensagem mensagem = new Mensagem();
        mensagem.setConversa(conversa);
        mensagem.setPapel(papel);
        mensagem.setConteudo(conteudo);
        mensagemRepository.save(mensagem);
    }

    public void definirTituloSeVazio(Conversa conversa, String primeiraPergunta){
        if(conversa.getTitulo() != null && !conversa.getTitulo().isEmpty()){
            return;
        }
        String titulo=primeiraPergunta.length()>TAMANHO_MAXIMO_TITULO
                ? primeiraPergunta.substring(0,TAMANHO_MAXIMO_TITULO)+"..."
                : primeiraPergunta;
        conversa.setTitulo(titulo);
        conversaRepository.save(conversa);
    }
}
