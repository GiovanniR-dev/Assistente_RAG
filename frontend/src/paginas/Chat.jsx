import { useState, useEffect, useRef } from 'react';
import { api } from '../api/client';

function Chat() {
  const [conversas, setConversas] = useState([]);
  const [conversaAtual, setConversaAtual] = useState(null);
  const [mensagens, setMensagens] = useState([]);
  const [pergunta, setPergunta] = useState('');
  const [pensando, setPensando] = useState(false);
  const [erro, setErro] = useState('');
  const fimDasMensagens = useRef(null);

  useEffect(() => {
    carregarConversas();
  }, []);

  useEffect(() => {
    fimDasMensagens.current?.scrollIntoView({ behavior: 'smooth' });
  }, [mensagens, pensando]);

  async function carregarConversas() {
    try {
      setConversas(await api.listarConversas());
    } catch (e) {
      setErro(e.message);
    }
  }

  async function abrirConversa(id) {
    setConversaAtual(id);
    setErro('');
    try {
      setMensagens(await api.listarMensagens(id));
    } catch (e) {
      setErro(e.message);
    }
  }

  async function novaConversa() {
    try {
      const conversa = await api.criarConversa();
      await carregarConversas();
      setConversaAtual(conversa.id);
      setMensagens([]);
    } catch (e) {
      setErro(e.message);
    }
  }

  async function enviar() {
    const texto = pergunta.trim();
    if (!texto || pensando) return;

    let id = conversaAtual;
    if (!id) {
      const conversa = await api.criarConversa();
      id = conversa.id;
      setConversaAtual(id);
    }

    setMensagens((atuais) => [...atuais, { papel: 'usuario', conteudo: texto }]);
    setPergunta('');
    setPensando(true);
    setErro('');

    try {
      const resposta = await api.enviarPergunta(id, texto);
      setMensagens((atuais) => [...atuais, resposta]);
      await carregarConversas();
    } catch (e) {
      setErro(e.message);
    } finally {
      setPensando(false);
    }
  }

  return (
    <div style={estilos.layout}>
      <aside style={estilos.lateral}>
        <button style={estilos.botaoNova} onClick={novaConversa}>
          + Nova conversa
        </button>

        {conversas.map((c) => (
          <div
            key={c.id}
            onClick={() => abrirConversa(c.id)}
            style={{
              ...estilos.itemConversa,
              background: c.id === conversaAtual ? '#e4e4e7' : 'transparent',
            }}
          >
            {c.titulo || 'Nova conversa'}
          </div>
        ))}
      </aside>

      <main style={estilos.principal}>
        <div style={estilos.mensagens}>
          {mensagens.length === 0 && !pensando && (
            <p style={estilos.vazio}>
              Faca uma pergunta sobre os documentos enviados.
            </p>
          )}

          {mensagens.map((m, i) => (
            <div
              key={i}
              style={{
                ...estilos.balao,
                ...(m.papel === 'usuario' ? estilos.balaoUsuario : estilos.balaoAssistente),
              }}
            >
              {m.conteudo}
            </div>
          ))}

          {pensando && (
            <div style={{ ...estilos.balao, ...estilos.balaoAssistente, color: '#a1a1aa' }}>
              Pensando...
            </div>
          )}

          <div ref={fimDasMensagens} />
        </div>

        {erro && <p style={estilos.erro}>{erro}</p>}

        <div style={estilos.entrada}>
          <input
            style={estilos.input}
            placeholder="Pergunte algo sobre seus documentos..."
            value={pergunta}
            onChange={(e) => setPergunta(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && enviar()}
            disabled={pensando}
          />
          <button style={estilos.botaoEnviar} onClick={enviar} disabled={pensando}>
            Enviar
          </button>
        </div>
      </main>
    </div>
  );
}

const estilos = {
  layout: { display: 'flex', height: 'calc(100vh - 53px)' },
  lateral: {
    width: 240,
    borderRight: '1px solid #e4e4e7',
    background: '#fafafa',
    padding: 12,
    overflowY: 'auto',
  },
  botaoNova: {
    width: '100%',
    padding: '8px',
    marginBottom: 12,
    background: '#18181b',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 13,
    cursor: 'pointer',
  },
  itemConversa: {
    padding: '8px 10px',
    borderRadius: 6,
    fontSize: 13,
    color: '#3f3f46',
    cursor: 'pointer',
    marginBottom: 2,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  principal: { flex: 1, display: 'flex', flexDirection: 'column', background: '#fff' },
  mensagens: { flex: 1, overflowY: 'auto', padding: 24 },
  balao: {
    maxWidth: '75%',
    padding: '10px 14px',
    borderRadius: 12,
    marginBottom: 12,
    fontSize: 14,
    lineHeight: 1.5,
    whiteSpace: 'pre-wrap',
  },
  balaoUsuario: { background: '#18181b', color: '#fff', marginLeft: 'auto' },
  balaoAssistente: { background: '#f4f4f5', color: '#18181b' },
  vazio: { color: '#a1a1aa', fontSize: 14, textAlign: 'center', marginTop: 40 },
  entrada: { display: 'flex', gap: 8, padding: 16, borderTop: '1px solid #e4e4e7' },
  input: {
    flex: 1,
    padding: '10px 14px',
    border: '1px solid #d4d4d8',
    borderRadius: 8,
    fontSize: 14,
  },
  botaoEnviar: {
    padding: '10px 20px',
    background: '#18181b',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 14,
    cursor: 'pointer',
  },
  erro: { color: '#dc2626', fontSize: 13, padding: '0 16px' },
};

export default Chat;