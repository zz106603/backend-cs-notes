export interface Category {
  name: string
  path: string
  documentCount: number
  children: Category[]
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

export interface CreateDocumentInput {
  title: string
  category: string
  content: string
}

export interface UpdateDocumentInput extends CreateDocumentInput {
  expectedUpdatedAt: string
}

export interface TrashDocument {
  id: string
  title: string
  originalPath: string
  deletedAt: string
}
