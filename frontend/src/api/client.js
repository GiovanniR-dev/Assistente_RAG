const BASE_URL = 'http://localhost:8080/api';

export function getToken() {
  return localStorage.getItem('token');
}

export function setToken(token) {
  localStorage.setItem('token', token);
}

export function limparToken() {
  localStorage.removeItem('token');
}

async function request(caminho, opcoes = {}) {
  const token = getToken();

  const headers = { ...opcoes.headers };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (opcoes.body && !(opcoes.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const resposta = await fetch(`${BASE_URL}${caminho}`, { ...opcoes, headers });

  if (resposta.status === 401 || resposta.status === 403) {
    limparToken();
    throw new Error('Sessao expirada. Faca login novamente.');
  }

  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => ({}));
    throw new Error(erro.erro || `Erro ${resposta.status}`);
  }

  if (resposta.status === 204) return null;
  return resposta.json();
}

export const api = {
  registrar: (nome, email, senha) =>
    request('/auth/registrar', {
      method: 'POST',
      body: JSON.stringify({ nome, email, senha }),
    }),

  login: (email, senha) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, senha }),
    }),

  listarDocumentos: () => request('/documentos'),

  enviarDocumento: (arquivo) => {
    const form = new FormData();
    form.append('arquivo', arquivo);
    return request('/documentos/upload', { method: 'POST', body: form });
  },

  criarConversa: () => request('/conversas', { method: 'POST' }),

  listarConversas: () => request('/conversas'),

  listarMensagens: (conversaId) => request(`/conversas/${conversaId}/mensagens`),

  enviarPergunta: (conversaId, pergunta) =>
    request(`/conversas/${conversaId}/mensagens`, {
      method: 'POST',
      body: JSON.stringify({ pergunta }),
    }),
};