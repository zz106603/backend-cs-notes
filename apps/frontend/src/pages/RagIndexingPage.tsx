import { useMutation } from '@tanstack/react-query'
import { CheckCircle2, CircleDollarSign, DatabaseZap, FileCheck2, FileMinus2, FilePenLine, FilePlus2, RefreshCw, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import type { RagIndexingAction, RagIndexingDocumentResult, RagIndexingResult } from '../types'

const ACTION_META: Record<RagIndexingAction, { label: string; icon: typeof FilePlus2 }> = {
  NEW: { label: '신규', icon: FilePlus2 },
  UPDATED: { label: '수정', icon: FilePenLine },
  DELETED: { label: '삭제', icon: FileMinus2 },
  UNCHANGED: { label: '최신', icon: FileCheck2 },
}

export function RagIndexingPage() {
  const [result, setResult] = useState<RagIndexingResult | null>(null)
  const preview = useMutation({
    mutationFn: api.previewRagIndex,
    onSuccess: setResult,
  })
  const execute = useMutation({
    mutationFn: api.executeRagIndex,
    onSuccess: setResult,
  })
  const pending = preview.isPending || execute.isPending
  const error = preview.error ?? execute.error
  const changes = result?.documents.filter((document) => document.action !== 'UNCHANGED') ?? []

  const runPreview = () => {
    execute.reset()
    preview.mutate()
  }

  const runIndexing = () => {
    if (!result?.dryRun || result.changedDocumentCount === 0 || pending) return
    const confirmed = window.confirm(
      `신규 임베딩 ${result.embeddedChunkCount.toLocaleString()}개를 생성하고 변경 문서를 pgvector에 반영할까요?`,
    )
    if (confirmed) execute.mutate()
  }

  return (
    <div className="page page--indexing">
      <section className="indexing-hero">
        <div className="eyebrow"><DatabaseZap size={14} /> VECTOR INDEX MANAGER</div>
        <h1>문서 색인 관리</h1>
        <p>먼저 무료 상태 확인으로 변경분을 계산하고, 확인한 문서와 Chunk만 pgvector에 반영합니다.</p>
        <div className="indexing-actions">
          <button className="secondary-button indexing-check-button" type="button" onClick={runPreview} disabled={pending}>
            <RefreshCw size={15} className={preview.isPending ? 'spin-icon' : ''} />
            {preview.isPending ? '변경 사항 확인 중' : result ? '상태 다시 확인' : '색인 상태 확인'}
          </button>
          <button
            className="primary-button"
            type="button"
            onClick={runIndexing}
            disabled={pending || !result?.dryRun || result.changedDocumentCount === 0}
          >
            <DatabaseZap size={15} className={execute.isPending ? 'spin-icon' : ''} />
            {execute.isPending ? '변경 문서 색인 중' : '변경 사항 색인'}
          </button>
        </div>
      </section>

      <div className="indexing-safety-note">
        <ShieldCheck size={17} />
        <span><strong>상태 확인은 OpenAI를 호출하지 않습니다.</strong> 실제 색인은 확인 창을 거친 뒤 신규 Chunk에만 비용이 발생합니다.</span>
      </div>

      {error && <ErrorState message={(error as Error).message} />}

      {pending && (
        <section className="rag-answer-loading" aria-live="polite" aria-busy="true">
          <span className="loader" />
          <div>
            <strong>{execute.isPending ? '변경된 문서를 색인하고 있습니다' : 'Markdown과 pgvector를 비교하고 있습니다'}</strong>
            <p>{execute.isPending ? '화면을 닫지 말고 완료될 때까지 기다려 주세요.' : '이 단계에서는 임베딩 비용이 발생하지 않습니다.'}</p>
          </div>
        </section>
      )}

      {!result && !pending && !error && (
        <section className="indexing-empty">
          <DatabaseZap size={34} />
          <h2>현재 색인 상태를 먼저 확인하세요</h2>
          <p>신규·수정·삭제 문서와 재사용 가능한 Chunk를 미리 계산합니다.</p>
        </section>
      )}

      {result && !pending && (
        <IndexingResultView result={result} changes={changes} />
      )}
    </div>
  )
}

function IndexingResultView({ result, changes }: { result: RagIndexingResult; changes: RagIndexingDocumentResult[] }) {
  const completed = !result.dryRun
  return (
    <div className="indexing-result">
      <header className={`indexing-result__status ${result.changedDocumentCount === 0 ? 'indexing-result__status--current' : ''}`}>
        <CheckCircle2 size={20} />
        <div>
          <strong>{completed ? '색인 업데이트가 완료되었습니다' : result.changedDocumentCount === 0 ? '색인이 최신 상태입니다' : `${result.changedDocumentCount}개 문서에 변경이 있습니다`}</strong>
          <span>{result.embeddingModel} · {completed ? '적용 결과' : '비용 없는 미리보기'}</span>
        </div>
      </header>

      <div className="indexing-metrics">
        <Metric label="변경 문서" value={result.changedDocumentCount} suffix="개" />
        <Metric label="신규 임베딩" value={result.embeddedChunkCount} suffix="Chunks" paid />
        <Metric label="벡터 재사용" value={result.reusedChunkCount} suffix="Chunks" />
        <Metric label="임베딩 입력" value={result.embeddingCharacterCount} suffix="chars" paid />
      </div>

      {result.embeddedChunkCount > 0 && result.dryRun && (
        <div className="indexing-cost-note">
          <CircleDollarSign size={16} />
          <span>실제 실행 시 <strong>{result.embeddedChunkCount.toLocaleString()}개 Chunk</strong>, 약 <strong>{result.embeddingCharacterCount.toLocaleString()}자</strong>만 OpenAI에 전송됩니다.</span>
        </div>
      )}

      <section className="indexing-change-section">
        <header>
          <div><span>{completed ? 'APPLIED CHANGES' : 'CHANGE PLAN'}</span><h2>{changes.length > 0 ? completed ? '반영된 문서' : '반영 대상 문서' : '반영할 변경 없음'}</h2></div>
          <small>전체 {result.documentCount} · 최신 {result.unchangedDocumentCount} · 삭제 {result.deletedDocumentCount}</small>
        </header>
        {changes.length === 0 ? (
          <div className="indexing-no-change"><FileCheck2 size={24} /><span>모든 문서와 Chunk가 현재 pgvector 상태와 일치합니다.</span></div>
        ) : (
          <div className="indexing-document-list">
            {changes.map((document) => <IndexingDocumentRow document={document} key={`${document.action}-${document.documentId}`} />)}
          </div>
        )}
      </section>
    </div>
  )
}

function Metric({ label, value, suffix, paid = false }: { label: string; value: number; suffix: string; paid?: boolean }) {
  return <div className={paid ? 'indexing-metric indexing-metric--paid' : 'indexing-metric'}><span>{label}</span><strong>{value.toLocaleString()}</strong><small>{suffix}</small></div>
}

function IndexingDocumentRow({ document }: { document: RagIndexingDocumentResult }) {
  const meta = ACTION_META[document.action]
  const Icon = meta.icon
  return (
    <article className={`indexing-document indexing-document--${document.action.toLowerCase()}`}>
      <div className="indexing-document__icon"><Icon size={17} /></div>
      <div className="indexing-document__content">
        <div><span>{meta.label}</span><strong>{document.documentTitle}</strong></div>
        <p>{document.documentPath || document.documentId}</p>
      </div>
      <div className="indexing-document__counts">
        <span>전체 {document.chunkCount}</span>
        {document.embeddedChunkCount > 0 && <strong>신규 {document.embeddedChunkCount}</strong>}
        {document.reusedChunkCount > 0 && <span>재사용 {document.reusedChunkCount}</span>}
      </div>
    </article>
  )
}
