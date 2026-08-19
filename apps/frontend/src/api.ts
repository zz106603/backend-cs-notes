import type { Category, CreateDocumentInput, DocumentDetail, DocumentSummary, TrashDocument, UpdateDocumentInput } from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: init?.body ? { 'Content-Type': 'application/json', ...init.headers } : init?.headers,
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail ?? (response.status === 404 ? '문서를 찾을 수 없습니다.' : '요청을 처리하지 못했습니다.'))
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
}
