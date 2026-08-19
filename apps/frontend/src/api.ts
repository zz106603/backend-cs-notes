import type { Category, DocumentDetail, DocumentSummary } from './types'

async function request<T>(path: string): Promise<T> {
  const response = await fetch(path)

  if (!response.ok) {
    throw new Error(response.status === 404 ? '문서를 찾을 수 없습니다.' : '데이터를 불러오지 못했습니다.')
  }

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
}
