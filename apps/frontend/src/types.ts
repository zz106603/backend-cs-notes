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
  tags: string[]
  excerpt?: string | null
}

export interface DocumentDetail extends DocumentSummary {
  content: string
}

export interface CreateDocumentInput {
  title: string
  category: string
  content: string
  tags: string[]
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

export interface RagSearchHit {
  chunkId: string
  documentId: string
  documentTitle: string
  documentPath: string
  tags: string[]
  sectionPath: string[]
  content: string
  score: number
  denseScore: number | null
  sparseScore: number | null
  denseRank: number | null
  sparseRank: number | null
  rerankScore: number | null
  rerankRank: number | null
  matchedBy: Array<'DENSE' | 'SPARSE'>
}

export interface RagSearchResponse {
  query: string
  embeddingModel: string | null
  mode: 'DENSE' | 'SPARSE' | 'HYBRID'
  limit: number
  minimumScore: number
  cachedQueryEmbedding: boolean
  results: RagSearchHit[]
}

export interface RagAnswerSource {
  number: number
  chunkId: string
  documentId: string
  documentTitle: string
  documentPath: string
  sectionPath: string[]
  score: number
}

export interface RagAnswerUsage {
  promptTokens?: number | null
  completionTokens?: number | null
  totalTokens?: number | null
}

export interface RagAnswerResponse {
  requestId: string
  question: string
  answer: string
  answerModel: string
  generated: boolean
  cached: boolean
  contextCharacters: number
  usage: RagAnswerUsage
  estimatedCostUsd: number
  sources: RagAnswerSource[]
}

export type RagIndexingAction = 'NEW' | 'UPDATED' | 'UNCHANGED' | 'DELETED'

export interface RagIndexingDocumentResult {
  documentId: string
  documentTitle: string
  documentPath: string
  action: RagIndexingAction
  chunkCount: number
  embeddedChunkCount: number
  reusedChunkCount: number
  embeddingCharacterCount: number
}

export interface RagIndexingResult {
  dryRun: boolean
  embeddingModel: string
  documentCount: number
  changedDocumentCount: number
  unchangedDocumentCount: number
  chunkCount: number
  embeddedChunkCount: number
  reusedChunkCount: number
  deletedDocumentCount: number
  embeddingCharacterCount: number
  documents: RagIndexingDocumentResult[]
}

export interface RagEvaluationCase {
  id: string
  query: string
  expectedDocumentPaths: string[]
  createdAt: string
}

export interface RagEvaluationModeResult {
  mode: 'DENSE' | 'SPARSE' | 'HYBRID'
  recallAtLimit: number
  firstRelevantRank: number | null
  reciprocalRank: number
  results: RagSearchHit[]
}

export interface RagEvaluationRunResponse {
  evaluationCase: RagEvaluationCase
  limit: number
  modes: RagEvaluationModeResult[]
}
