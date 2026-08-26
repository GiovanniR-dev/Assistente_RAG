import { useState, useEffect, useRef } from 'react';
import { api } from '../api/client';

function Documentos() {
  const [documentos, setDocumentos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const inputArquivo = useRef(null);

  useEffect(() => {
    carregarDocumentos();
  }, []);

  async function carregarDocumentos() {
    try {
      const lista = await api.listarDocumentos();
      setDocumentos(lista);
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  async function enviarArquivo(evento) {
    const arquivo = evento.target.files[0];
    if (!arquivo) return;

    setErro('');
    setEnviando(true);

    try {
      await api.enviarDocumento(arquivo);
      await carregarDocumentos();
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
      evento.target.value = '';
    }
  }

  return (
    <div style={estilos.container}>
      <div style={estilos.cabecalho}>
        <h2 style={estilos.titulo}>Documentos</h2>
        <button
          style={estilos.botao}
          onClick={() => inputArquivo.current.click()}
          disabled={enviando}
        >
          {enviando ? 'Processando...' : 'Enviar PDF'}
        </button>
      </div>

      <input
        ref={inputArquivo}
        type="file"
        accept="application/pdf"
        style={{ display: 'none' }}
        onChange={enviarArquivo}
      />

      {enviando && (
        <p style={estilos.aviso}>
          Extraindo texto e gerando embeddings. Isso pode levar alguns segundos.
        </p>
      )}

      {erro && <p style={estilos.erro}>{erro}</p>}

      {carregando ? (
        <p style={estilos.vazio}>Carregando...</p>
      ) : documentos.length === 0 ? (
        <p style={estilos.vazio}>Nenhum documento ainda. Envie um PDF para comecar.</p>
      ) : (
        <ul style={estilos.lista}>
          {documentos.map((doc) => (
            <li key={doc.id} style={estilos.item}>
              {doc.nomeArquivo}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const estilos = {
  container: { maxWidth: 640, margin: '0 auto', padding: 24 },
  cabecalho: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  titulo: { fontSize: 18, color: '#18181b', margin: 0 },
  botao: {
    padding: '8px 16px',
    background: '#18181b',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    fontSize: 14,
    cursor: 'pointer',
  },
  lista: { listStyle: 'none', padding: 0, marginTop: 20 },
  item: {
    padding: '12px 16px',
    background: '#fff',
    border: '1px solid #e4e4e7',
    borderRadius: 8,
    marginBottom: 8,
    fontSize: 14,
    color: '#3f3f46',
  },
  vazio: { color: '#71717a', fontSize: 14, marginTop: 20 },
  aviso: { color: '#71717a', fontSize: 13, marginTop: 16 },
  erro: { color: '#dc2626', fontSize: 13, marginTop: 16 },
};

export default Documentos;