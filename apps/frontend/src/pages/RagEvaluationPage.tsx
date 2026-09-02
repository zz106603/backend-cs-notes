import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, FlaskConical, Play, Plus, Search, Trash2, X } from 'lucide-react'
import { useMemo, useState } from 'react'
import { api } from '../api'
import { LoadingState } from '../components/LoadingState'
import type { RagEvaluationModeResult, RagEvaluationRunResponse } from '../types'

const MODE_LABEL = { DENSE: '의미 검색', SPARSE: '키워드 검색', HYBRID: '통합 검색' } as const

export function RagEvaluationPage() {
  const queryClient = useQueryClient()
  const [question, setQuestion] = useState('')
  const [documentQuery, setDocumentQuery] = useState('')
  const [expectedPaths, setExpectedPaths] = useState<string[]>([])
  const [result, setResult] = useState<RagEvaluationRunResponse | null>(null)
  const cases = useQuery({ queryKey: ['rag-evaluations'], queryFn: api.ragEvaluationCases })
  const documents = useQuery({ queryKey: ['documents', 'evaluation'], queryFn: () => api.documents() })
  const create = useMutation({
    mutationFn: () => api.createRagEvaluationCase(question, expectedPaths),
    onSuccess: async () => {
      setQuestion('')
      setExpectedPaths([])
      setDocumentQuery('')
      await queryClient.invalidateQueries({ queryKey: ['rag-evaluations'] })
    },
  })
  const run = useMutation({ mutationFn: api.runRagEvaluation, onSuccess: setResult })
  const remove = useMutation({
    mutationFn: api.deleteRagEvaluationCase,
    onSuccess: async (_, id) => {
      if (result?.evaluationCase.id === id) setResult(null)
      await queryClient.invalidateQueries({ queryKey: ['rag-evaluations'] })
    },
  })
  const filteredDocuments = useMemo(() => {
    const keyword = documentQuery.trim().toLowerCase()
    if (!keyword) return documents.data?.slice(0, 8) ?? []
    return documents.data?.filter((document) =>
      `${document.title} ${document.path}`.toLowerCase().includes(keyword)).slice(0, 8) ?? []
  }, [documentQuery, documents.data])
  const error = cases.error ?? documents.error ?? create.error ?? run.error ?? remove.error

  const toggleExpectedPath = (path: string) => {
    setExpectedPaths((current) => current.includes(path)
      ? current.filter((expectedPath) => expectedPath !== path)
      : [...current, path])
  }

  const deleteCase = (id: string) => {
    if (window.confirm('이 평가 질문을 삭제할까요?')) remove.mutate(id)
  }

  return (
    <div className="page page--evaluation">
      <section className="evaluation-hero">
        <div className="eyebrow"><FlaskConical size={14} /> RETRIEVAL EVALUATION LAB</div>
        <h1>검색 품질 평가</h1>
        <p>질문과 기대 문서를 기준으로 Dense, Sparse, Hybrid의 검색 순위를 같은 조건에서 비교합니다.</p>
      </section>

      <div className="evaluation-note">
        <strong>Recall@10</strong>은 기대 문서를 얼마나 찾았는지, <strong>첫 정답 순위</strong>는 가장 먼저 나온 기대 문서의 위치를 뜻합니다. 실행 시 질의 임베딩은 캐시를 통해 한 번만 생성합니다.
      </div>

      {error && <div className="evaluation-error">{(error as Error).message}</div>}

      <section className="evaluation-create-panel">
        <header><span>NEW TEST CASE</span><h2>평가 질문 만들기</h2></header>
        <label>
          <span>질문</span>
          <input value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="예: 새로운 트랜잭션으로 분리해서 실행하려면?" maxLength={500} />
        </label>
        <div className="evaluation-document-picker">
          <span>기대 문서 <small>선택하지 않으면 결과가 없어야 하는 부정 평가로 저장됩니다.</small></span>
          <div className="evaluation-document-search"><Search size={15} /><input value={documentQuery} onChange={(event) => setDocumentQuery(event.target.value)} placeholder="문서 제목 또는 경로 검색" /></div>
          <div className="evaluation-document-options">
            {filteredDocuments.map((document) => {
              const selected = expectedPaths.includes(document.path)
              return <button type="button" className={selected ? 'selected' : ''} onClick={() => toggleExpectedPath(document.path)} key={document.id}>
                <span className="evaluation-check">{selected && <Check size={12} />}</span><span><strong>{document.title}</strong><small>{document.path}</small></span>
              </button>
            })}
          </div>
          {expectedPaths.length > 0 && <div className="evaluation-selected-paths">
            {expectedPaths.map((path) => <span key={path}>{path}<button type="button" aria-label={`${path} 선택 해제`} onClick={() => toggleExpectedPath(path)}><X size={11} /></button></span>)}
          </div>}
        </div>
        <button className="primary-button" type="button" disabled={!question.trim() || create.isPending} onClick={() => create.mutate()}>
          <Plus size={15} /> {create.isPending ? '저장 중' : '평가 케이스 저장'}
        </button>
      </section>

      <section className="evaluation-case-section">
        <header><div><span>SAVED TEST CASES</span><h2>평가 질문</h2></div><small>{cases.data?.length ?? 0} cases</small></header>
        {cases.isLoading && <LoadingState />}
        {cases.data?.length === 0 && <div className="evaluation-empty"><FlaskConical size={25} /><p>첫 평가 질문과 기대 문서를 등록해 보세요.</p></div>}
        <div className="evaluation-case-list">
          {cases.data?.map((evaluationCase) => <article className="evaluation-case" key={evaluationCase.id}>
            <div><strong>{evaluationCase.query}</strong><p>{evaluationCase.expectedDocumentPaths.length > 0 ? evaluationCase.expectedDocumentPaths.join(' · ') : '관련 문서 없음 · 부정 평가'}</p></div>
            <div className="evaluation-case-actions">
              <button type="button" className="secondary-button" disabled={run.isPending} onClick={() => run.mutate(evaluationCase.id)}><Play size={13} /> 비교 실행</button>
              <button type="button" className="icon-button" aria-label="평가 질문 삭제" onClick={() => deleteCase(evaluationCase.id)}><Trash2 size={14} /></button>
            </div>
          </article>)}
        </div>
      </section>

      {run.isPending && <LoadingState />}
      {result && <EvaluationResult result={result} />}
    </div>
  )
}

function EvaluationResult({ result }: { result: RagEvaluationRunResponse }) {
  const expected = new Set(result.evaluationCase.expectedDocumentPaths)
  return <section className="evaluation-results">
    <header><span>COMPARISON RESULT</span><h2>“{result.evaluationCase.query}”</h2></header>
    <div className="evaluation-result-grid">
      {result.modes.map((modeResult) => <ModeResult result={modeResult} expected={expected} limit={result.limit} key={modeResult.mode} />)}
    </div>
  </section>
}

function ModeResult({ result, expected, limit }: { result: RagEvaluationModeResult; expected: Set<string>; limit: number }) {
  const negativeCase = expected.size === 0
  const hasCandidates = result.results.length > 0
  return <article className={`evaluation-mode evaluation-mode--${result.mode.toLowerCase()}`}>
    <header><span>{result.mode}</span><h3>{MODE_LABEL[result.mode]}</h3></header>
    <div className="evaluation-metrics">
      {negativeCase ? <>
        <div><span>부정 질문 진단</span><strong>{hasCandidates ? '관련성 판정 필요' : '후보 없음'}</strong></div>
        <div><span>반환 결과</span><strong>{result.results.length}개</strong></div>
        <div><span>판정 기준</span><strong>후보 관찰</strong></div>
      </> : <>
        <div><span>Recall@{limit}</span><strong>{(result.recallAtLimit * 100).toFixed(0)}%</strong></div>
        <div><span>첫 정답 순위</span><strong>{result.firstRelevantRank ? `${result.firstRelevantRank}위` : '없음'}</strong></div>
        <div><span>Reciprocal Rank</span><strong>{result.reciprocalRank.toFixed(3)}</strong></div>
      </>}
    </div>
    <ol className="evaluation-ranking">
      {result.results.map((hit, index) => <li className={expected.has(hit.documentPath) ? 'relevant' : ''} key={hit.chunkId}>
        <span className="evaluation-rank">{index + 1}</span>
        <div className="evaluation-hit">
          <strong>{hit.documentTitle}</strong>
          <small className="evaluation-hit-path">{hit.documentPath}</small>
          <div className="evaluation-hit-meta">
            <span>{hit.sectionPath.length > 0 ? hit.sectionPath.join(' › ') : '문서 본문'}</span>
            <code title={hit.chunkId}>Chunk {hit.chunkId.slice(0, 8)}</code>
            {hit.rerankScore !== null && hit.rerankRank !== null &&
              <code>Rerank {hit.rerankRank}위 · {hit.rerankScore.toFixed(3)}</code>}
          </div>
          <p title={hit.content}>{hit.content.replace(/\s+/g, ' ').trim()}</p>
        </div>
        {expected.has(hit.documentPath) && <Check size={14} aria-label="기대 문서" />}
      </li>)}
    </ol>
  </article>
}
