import { useState } from 'react';
import { api, setToken } from '../api/client';

function Login({ aoEntrar }) {
  const [modo, setModo] = useState('login');
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  async function enviar() {
    setErro('');
    setCarregando(true);

    try {
      if (modo === 'registro') {
        await api.registrar(nome, email, senha);
      }
      const dados = await api.login(email, senha);
      setToken(dados.token);
      aoEntrar();
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div style={estilos.container}>
      <div style={estilos.caixa}>
        <h1 style={estilos.titulo}>Assistente de Documentos</h1>

        {modo === 'registro' && (
          <input
            style={estilos.input}
            placeholder="Nome"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        )}

        <input
          style={estilos.input}
          type="email"
          placeholder="E-mail"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          style={estilos.input}
          type="password"
          placeholder="Senha"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && enviar()}
        />

        {erro && <p style={estilos.erro}>{erro}</p>}

        <button style={estilos.botao} onClick={enviar} disabled={carregando}>
          {carregando ? 'Aguarde...' : modo === 'login' ? 'Entrar' : 'Criar conta'}
        </button>

        <p style={estilos.alternar}>
          {modo === 'login' ? 'Nao tem conta? ' : 'Ja tem conta? '}
          <span
            style={estilos.link}
            onClick={() => { setModo(modo === 'login' ? 'registro' : 'login'); setErro(''); }}
          >
            {modo === 'login' ? 'Criar uma' : 'Entrar'}
          </span>
        </p>
      </div>
    </div>
  );
}

const estilos = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#f4f4f5',
    fontFamily: 'system-ui, sans-serif',
  },
  caixa: {
    background: '#fff',
    padding: 32,
    borderRadius: 12,
    width: 320,
    boxShadow: '0 2px 16px rgba(0,0,0,0.08)',
  },
  titulo: { fontSize: 20, marginTop: 0, marginBottom: 24, color: '#18181b' },
  input: {
    width: '100%',
    padding: '10px 12px',
    marginBottom: 12,
    border: '1px solid #d4d4d8',
    borderRadius: 6,
    fontSize: 14,
    boxSizing: 'border-box',
  },
  botao: {
    width: '100%',
    padding: '10px',
    background: '#18181b',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    cursor: 'pointer',
  },
  erro: { color: '#dc2626', fontSize: 13, margin: '0 0 12px' },
  alternar: { fontSize: 13, textAlign: 'center', marginBottom: 0, color: '#71717a' },
  link: { color: '#18181b', cursor: 'pointer', textDecoration: 'underline' },
};

export default Login;