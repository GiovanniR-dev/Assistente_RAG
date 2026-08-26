import { useState } from 'react';
import { getToken, limparToken } from './api/client';
import Login from './paginas/Login';
import Documentos from './paginas/Documentos';
import Chat from './paginas/Chat';

function App() {
  const [autenticado, setAutenticado] = useState(!!getToken());
  const [aba, setAba] = useState('chat');

  if (!autenticado) {
    return <Login aoEntrar={() => setAutenticado(true)} />;
  }

  return (
    <div style={{ minHeight: '100vh', fontFamily: 'system-ui, sans-serif' }}>
      <header style={cabecalho.barra}>
        <strong style={cabecalho.marca}>Assistente RAG</strong>

        <nav style={cabecalho.nav}>
          <button
            style={{ ...cabecalho.aba, ...(aba === 'chat' ? cabecalho.abaAtiva : {}) }}
            onClick={() => setAba('chat')}
          >
            Chat
          </button>
          <button
            style={{ ...cabecalho.aba, ...(aba === 'documentos' ? cabecalho.abaAtiva : {}) }}
            onClick={() => setAba('documentos')}
          >
            Documentos
          </button>
        </nav>

        <button
          style={cabecalho.sair}
          onClick={() => { limparToken(); setAutenticado(false); }}
        >
          Sair
        </button>
      </header>

      {aba === 'chat' ? <Chat /> : <Documentos />}
    </div>
  );
}

const cabecalho = {
  barra: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 24px',
    background: '#fff',
    borderBottom: '1px solid #e4e4e7',
    height: 28,
  },
  marca: { fontSize: 15, color: '#18181b' },
  nav: { display: 'flex', gap: 4 },
  aba: {
    padding: '6px 14px',
    background: 'none',
    border: 'none',
    borderRadius: 6,
    fontSize: 13,
    color: '#71717a',
    cursor: 'pointer',
  },
  abaAtiva: { background: '#f4f4f5', color: '#18181b' },
  sair: { background: 'none', border: 'none', cursor: 'pointer', color: '#71717a', fontSize: 13 },
};

export default App;