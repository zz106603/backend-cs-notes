import type { Category, CreateDocumentInput, DocumentDetail, DocumentSummary, RagAnswerResponse, RagIndexingResult, RagSearchResponse, TrashDocument, UpdateDocumentInput } from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: init?.body ? { 'Content-Type': 'application/json', ...init.headers } : init?.headers,
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    const notFoundMessage = path.startsWith('/api/rag/index')
      ? '문서 색인 기능이 비활성화되어 있습니다. 백엔드의 RAG 색인 설정을 확인해 주세요.'
      : path === '/api/rag/answer'
      ? 'RAG 답변 기능이 비활성화되어 있습니다. 백엔드의 답변 생성 설정을 확인해 주세요.'
      : path.startsWith('/api/rag/')
        ? '의미 검색 기능이 비활성화되어 있습니다. 백엔드의 RAG 검색 설정을 확인해 주세요.'
        : '문서를 찾을 수 없습니다.'
    throw new Error(problem?.detail ?? (response.status === 404 ? notFoundMessage : '요청을 처리하지 못했습니다.'))
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  categories: () => request<Category[]>('/api/categories'),
  documents: (category?: string, query?: string) => {
    const params = new URLSearchParams()
    if (category) params.set('category', category)
    if (query) params.set('query', query)
    const search = params.size ? `?${params.toString()}` : ''
    return request<DocumentSummary[]>(`/api/documents${search}`)
  },
  document: (id: string) => request<DocumentDetail>(`/api/documents/${encodeURIComponent(id)}`),
  createDocument: (input: CreateDocumentInput) => request<DocumentDetail>('/api/documents', {
    method: 'POST',
    body: JSON.stringify(input),
  }),
  updateDocument: (id: string, input: UpdateDocumentInput) => request<DocumentDetail>(
    `/api/documents/${encodeURIComponent(id)}`,
    { method: 'PUT', body: JSON.stringify(input) },
  ),
  moveDocumentToTrash: (id: string) => request<void>(`/api/documents/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  }),
  trashDocuments: () => request<TrashDocument[]>('/api/trash'),
  permanentlyDeleteTrashDocument: (id: string) => request<void>(`/api/trash/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  }),
  restoreTrashDocument: (id: string) => request<DocumentDetail>(`/api/trash/${encodeURIComponent(id)}/restore`, {
    method: 'POST',
  }),
  semanticSearch: (query: string) => request<RagSearchResponse>('/api/rag/search', {
    method: 'POST',
    body: JSON.stringify({ query, limit: 10, minimumScore: 0.0 }),
  }),
  ragAnswer: (question: string) => request<RagAnswerResponse>('/api/rag/answer', {
    method: 'POST',
    body: JSON.stringify({ question, sourceLimit: 4, minimumScore: 0.3 }),
  }),
  previewRagIndex: () => request<RagIndexingResult>('/api/rag/index', { method: 'POST' }),
  executeRagIndex: () => request<RagIndexingResult>('/api/rag/index?dryRun=false', { method: 'POST' }),
}
