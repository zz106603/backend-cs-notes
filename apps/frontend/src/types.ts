export interface Category {
  name: string
  documentCount: number
}

export interface DocumentSummary {
  id: string
  title: string
  category: string
  path: string
  updatedAt: string
}

export interface DocumentDetail extends DocumentSummary {
  content: string
}
