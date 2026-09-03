import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ArrowRight, BrainCircuit, FileText, ListFilter, Search, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { FormEvent, MouseEvent } from 'react'
import { api } from '../api'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { RagSearchHit } from '../types'

type SearchMode = 'general' | 'hybrid' | 'dense' | 'sparse'

function effectiveScore(hit: RagSearchHit) {
  return hit.rerankScore ?? hit.score
}

function groupSemanticResults(results: RagSearchHit[]) {
  const grouped = new Map<string, {
    documentId: string
    title: string
    path: string
    bestScore: number
    sectionPath: string[]
  }>()
  results.forEach((hit) => {
    const score = effectiveScore(hit)
    const existing = grouped.get(hit.documentId)
    if (!existing || score > existing.bestScore) {
      grouped.set(hit.documentId, {
        documentId: hit.documentId,
        title: hit.documentTitle,
        path: hit.documentPath,
        bestScore: score,
        sectionPath: hit.sectionPath,
      })
    }
  })
  return [...grouped.values()].sort((left, right) => right.bestScore - left.bestScore)
}

export function GlobalSearchModal({ onClose }: { onClose: () => void }) {
  const [mode, setMode] = useState<SearchMode>('general')
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query.trim(), 250)
  const generalSearch = useQuery({
    queryKey: ['global-search', debouncedQuery],
    queryFn: () => api.documents(undefined, debouncedQuery),
    enabled: mode === 'general' && debouncedQuery.length > 0,
    placeholderData: (previousData) => previousData,
  })
  const ragSearch = useMutation({
    mutationFn: ({ searchQuery, searchMode }: { searchQuery: string; searchMode: 'DENSE' | 'SPARSE' | 'HYBRID' }) =>
      api.ragSearch(searchQuery, searchMode),
  })
  const semanticDocuments = groupSemanticResults(ragSearch.data?.results ?? [])

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [onClose])

  const changeMode = (nextMode: SearchMode) => {
    setMode(nextMode)
    ragSearch.reset()
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (mode !== 'general' && query.trim()) {
      ragSearch.mutate({ searchQuery: query.trim(), searchMode: mode.toUpperCase() as 'HYBRID' | 'DENSE' | 'SPARSE' })
    }
  }

  const closeFromBackdrop = (event: MouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) onClose()
  }

  return createPortal(
    <div className="global-search-backdrop" role="presentation" onMouseDown={closeFromBackdrop}>
      <section className="global-search-modal" role="dialog" aria-modal="true" aria-labelledby="global-search-title">
        <header className="global-search-header">
          <div>
            <span>QUICK SEARCH</span>
            <h2 id="global-search-title">전체 문서 검색</h2>
          </div>
          <button type="button" aria-label="검색 닫기" title="닫기" onClick={onClose}><X size={19} /></button>
        </header>

        <div className="global-search-mode" role="tablist" aria-label="검색 방식">
          <button type="button" role="tab" aria-selected={mode === 'general'} className={mode === 'general' ? 'active' : ''} onClick={() => changeMode('general')}>
            <ListFilter size={14} /> 일반 검색
          </button>
          <button type="button" role="tab" aria-selected={mode === 'hybrid'} className={mode === 'hybrid' ? 'active' : ''} onClick={() => changeMode('hybrid')}>
            <Search size={14} /> 통합 검색
          </button>
          <button type="button" role="tab" aria-selected={mode === 'dense'} className={mode === 'dense' ? 'active' : ''} onClick={() => changeMode('dense')}>
            <BrainCircuit size={14} /> 의미 검색
          </button>
          <button type="button" role="tab" aria-selected={mode === 'sparse'} className={mode === 'sparse' ? 'active' : ''} onClick={() => changeMode('sparse')}>
            <Search size={14} /> 키워드 검색
          </button>
        </div>

        <form className={`global-search-form ${mode !== 'general' ? 'global-search-form--semantic' : ''}`} onSubmit={submit}>
          {mode === 'dense' ? <BrainCircuit size={19} /> : <Search size={19} />}
          <input
            autoFocus
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={mode === 'hybrid' ? '개념과 핵심 키워드를 함께 입력하세요' : mode === 'dense' ? '찾고 싶은 개념을 문장으로 입력하세요' : mode === 'sparse' ? '정확한 기술 용어나 키워드를 입력하세요' : '제목, 태그 또는 본문 검색'}
            aria-label={mode === 'hybrid' ? '통합 검색어' : mode === 'dense' ? '의미 검색어' : mode === 'sparse' ? '키워드 검색어' : '일반 검색어'}
          />
          {mode !== 'general' && (
            <button type="submit" disabled={!query.trim() || ragSearch.isPending}>
              {ragSearch.isPending ? '검색 중' : '검색'}
            </button>
          )}
        </form>

        {mode !== 'general' && <p className="global-search-cost-note">{mode === 'hybrid' ? '의미와 키워드 결과를 함께 반영하며 질의 임베딩 비용이 발생합니다.' : mode === 'dense' ? 'Enter 또는 검색 버튼을 눌렀을 때만 임베딩 API를 호출합니다.' : '키워드 검색은 OpenAI 비용이 발생하지 않습니다.'}</p>}

        <div className="global-search-results" aria-live="polite">
          {!query.trim() && (
            <div className="global-search-empty"><Search size={25} /><p>어느 페이지에서든 문서를 바로 찾아보세요.</p></div>
          )}

          {mode === 'general' && query.trim() && generalSearch.isLoading && <div className="global-search-status"><span className="loader" /> 문서를 찾는 중</div>}
          {mode === 'general' && generalSearch.error && <div className="global-search-error">{(generalSearch.error as Error).message}</div>}
          {mode === 'general' && generalSearch.data?.length === 0 && <div className="global-search-empty"><p>일치하는 문서가 없습니다.</p></div>}
          {mode === 'general' && generalSearch.data?.map((document) => (
            <Link className="global-search-result" to={`/notes/${document.id}`} onClick={onClose} key={document.id}>
              <span className="global-search-result__icon"><FileText size={17} /></span>
              <span className="global-search-result__content">
                <strong>{document.title}</strong>
                <small>{document.path}</small>
              </span>
              <ArrowRight size={15} />
            </Link>
          ))}

          {mode !== 'general' && ragSearch.isPending && <div className="global-search-status"><span className="loader" /> {mode === 'hybrid' ? '의미와 키워드가 관련된' : mode === 'dense' ? '의미가 가까운' : '키워드가 일치하는'} 문서를 찾는 중</div>}
          {mode !== 'general' && ragSearch.error && <div className="global-search-error">{(ragSearch.error as Error).message}</div>}
          {mode !== 'general' && ragSearch.data && semanticDocuments.length === 0 && <div className="global-search-empty"><p>
            {ragSearch.data.rerankingApplied && ragSearch.data.rerankingMinimumScore !== null
              ? `관련도 ${(ragSearch.data.rerankingMinimumScore * 100).toFixed(0)}% 이상인 문서를 찾지 못했습니다.`
              : '관련 문서를 찾지 못했습니다.'}
          </p></div>}
          {mode !== 'general' && semanticDocuments.map((document) => (
            <Link className="global-search-result" to={`/notes/${document.documentId}`} onClick={onClose} key={document.documentId}>
              <span className="global-search-result__icon global-search-result__icon--semantic"><BrainCircuit size={17} /></span>
              <span className="global-search-result__content">
                <strong>{document.title}</strong>
                <small>{document.sectionPath.length > 0 ? document.sectionPath.join(' › ') : document.path}</small>
              </span>
              <span className="global-search-score">{(document.bestScore * 100).toFixed(1)}%</span>
            </Link>
          ))}
        </div>

        <footer className="global-search-footer"><kbd>ESC</kbd><span>닫기</span><kbd>↵</kbd><span>검색</span></footer>
      </section>
    </div>,
    document.body,
  )
}
