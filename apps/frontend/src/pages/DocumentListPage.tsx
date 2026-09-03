import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ArrowRight, ArrowUpRight, BrainCircuit, FilePlus2, FileText, Grid2X2, List, ListFilter, Search, Sparkles } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { usePersistentState } from '../hooks/usePersistentState'
import type { RagSearchHit } from '../types'

type SearchMode = 'general' | 'hybrid' | 'dense' | 'sparse'

function toRagMode(mode: Exclude<SearchMode, 'general'>) {
  return mode.toUpperCase() as 'HYBRID' | 'DENSE' | 'SPARSE'
}

function matchedByLabel(hit: RagSearchHit) {
  if (hit.rerankScore !== null && hit.rerankRank !== null) {
    return `재정렬 ${hit.rerankRank}위 · ${hit.rerankScore.toFixed(3)}`
  }
  return [
    hit.denseRank ? `의미 ${hit.denseRank}위` : null,
    hit.sparseRank ? `키워드 ${hit.sparseRank}위` : null,
  ].filter(Boolean).join(' · ')
}

function effectiveScore(hit: RagSearchHit) {
  return hit.rerankScore ?? hit.score
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric' }).format(new Date(value))
}

function groupSemanticResults(results: RagSearchHit[]) {
  const grouped = new Map<string, {
    documentId: string
    title: string
    path: string
    tags: string[]
    bestScore: number
    reranked: boolean
    hits: RagSearchHit[]
  }>()

  results.forEach((hit) => {
    const score = effectiveScore(hit)
    const existing = grouped.get(hit.documentId)
    if (existing) {
      existing.bestScore = Math.max(existing.bestScore, score)
      existing.reranked ||= hit.rerankScore !== null
      existing.hits.push(hit)
      return
    }
    grouped.set(hit.documentId, {
      documentId: hit.documentId,
      title: hit.documentTitle,
      path: hit.documentPath,
      tags: hit.tags,
      bestScore: score,
      reranked: hit.rerankScore !== null,
      hits: [hit],
    })
  })
  return [...grouped.values()]
}

export function DocumentListPage() {
  const [searchParams] = useSearchParams()
  const category = searchParams.get('category') ?? undefined
  const [query, setQuery] = useState('')
  const [searchMode, setSearchMode] = useState<SearchMode>('general')
  const [viewMode, setViewMode] = usePersistentState<'cards' | 'list'>('cs-notes-library-view', 'cards')
  const debouncedQuery = useDebouncedValue(query, 250)
  const { data: documents, isLoading, error } = useQuery({
    queryKey: ['documents', category, debouncedQuery],
    queryFn: () => api.documents(category, debouncedQuery),
    placeholderData: (previousData) => previousData,
    enabled: searchMode === 'general',
  })
  const ragSearch = useMutation({
    mutationFn: ({ searchQuery, mode }: { searchQuery: string; mode: 'DENSE' | 'SPARSE' | 'HYBRID' }) =>
      api.ragSearch(searchQuery, mode),
  })
  const semanticDocuments = groupSemanticResults(ragSearch.data?.results ?? [])

  const changeSearchMode = (mode: SearchMode) => {
    setSearchMode(mode)
    ragSearch.reset()
  }

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    if (searchMode !== 'general' && query.trim()) {
      ragSearch.mutate({ searchQuery: query.trim(), mode: toRagMode(searchMode) })
    }
  }

  return (
    <div className="page page--library">
      <section className="library-hero">
        <div className="eyebrow"><Sparkles size={14} /> PERSONAL KNOWLEDGE ARCHIVE</div>
        <h1>{category ?? '모든 CS 노트'}</h1>
        <p>{category ? `${category}에 관해 정리한 개념을 살펴보세요.` : '배운 것을 기록하고, 필요할 때 다시 꺼내보는 백엔드 지식 저장소입니다.'}</p>

        <div className="search-mode-switch" role="tablist" aria-label="검색 방식">
          <button
            type="button"
            role="tab"
            aria-selected={searchMode === 'general'}
            className={searchMode === 'general' ? 'active' : ''}
            onClick={() => changeSearchMode('general')}
          >
            <ListFilter size={14} /> 일반 검색
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={searchMode === 'hybrid'}
            className={searchMode === 'hybrid' ? 'active' : ''}
            onClick={() => changeSearchMode('hybrid')}
          >
            <Sparkles size={14} /> 통합 검색
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={searchMode === 'dense'}
            className={searchMode === 'dense' ? 'active' : ''}
            onClick={() => changeSearchMode('dense')}
          >
            <BrainCircuit size={14} /> 의미 검색
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={searchMode === 'sparse'}
            className={searchMode === 'sparse' ? 'active' : ''}
            onClick={() => changeSearchMode('sparse')}
          >
            <Search size={14} /> 키워드 검색
          </button>
        </div>

        <form className={`search-box ${searchMode !== 'general' ? 'search-box--semantic' : ''}`} onSubmit={submitSearch}>
          {searchMode === 'dense' ? <BrainCircuit size={20} /> : <Search size={20} />}
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={searchMode === 'hybrid' ? '개념과 핵심 키워드를 함께 입력하세요' : searchMode === 'dense' ? '의미나 개념을 문장으로 질문해 보세요' : searchMode === 'sparse' ? '정확한 기술 용어나 키워드를 입력하세요' : '제목, 태그 또는 본문 검색'}
            aria-label={searchMode === 'hybrid' ? '통합 검색' : searchMode === 'dense' ? '의미 검색' : searchMode === 'sparse' ? '키워드 검색' : '문서 검색'}
          />
          {searchMode !== 'general' ? (
            <button className="semantic-search-button" type="submit" disabled={!query.trim() || ragSearch.isPending}>
              {ragSearch.isPending ? '검색 중' : '검색'}
            </button>
          ) : <kbd>/</kbd>}
        </form>
        {searchMode !== 'general' && (
          <p className="semantic-search-note">
            {searchMode === 'hybrid' ? '의미 검색과 키워드 검색 결과를 RRF 순위로 합칩니다. 질의 임베딩 비용이 발생합니다.' : searchMode === 'dense' ? 'Enter 또는 검색 버튼을 누를 때만 임베딩 API를 호출합니다.' : '키워드 검색은 PostgreSQL FTS를 사용하며 OpenAI 비용이 발생하지 않습니다.'}
            {category ? ' 검색은 선택한 폴더와 관계없이 전체 문서를 대상으로 합니다.' : ''}
          </p>
        )}
      </section>

      <section className="library-section">
        <header className="section-header">
          <div>
            <span>{searchMode === 'hybrid' ? 'HYBRID RETRIEVAL' : searchMode === 'dense' ? 'DENSE RETRIEVAL' : searchMode === 'sparse' ? 'SPARSE RETRIEVAL' : 'COLLECTION'}</span>
            <h2>{searchMode !== 'general'
              ? ragSearch.data ? `'${ragSearch.data.query}' 관련 문서` : searchMode === 'hybrid' ? '의미와 키워드로 함께 찾기' : searchMode === 'dense' ? '의미로 문서 찾기' : '키워드로 문서 찾기'
              : debouncedQuery ? `'${debouncedQuery}' 검색 결과` : category ? `${category} 문서` : '전체 문서'}</h2>
          </div>
          <div className="section-actions">
            <p>{searchMode !== 'general' ? `${semanticDocuments.length} related notes` : `${documents?.length ?? 0} notes`}</p>
            {searchMode === 'general' && (
              <div className="view-mode-switch" role="group" aria-label="문서 보기 방식">
                <button type="button" className={viewMode === 'cards' ? 'active' : ''} onClick={() => setViewMode('cards')} aria-label="카드형 보기"><Grid2X2 size={14} /></button>
                <button type="button" className={viewMode === 'list' ? 'active' : ''} onClick={() => setViewMode('list')} aria-label="목록형 보기"><List size={15} /></button>
              </div>
            )}
            <Link to="/notes/new" className="primary-button primary-button--small">
              <FilePlus2 size={15} /> 새 문서
            </Link>
          </div>
        </header>

        {searchMode === 'general' && isLoading && <LoadingState />}
        {searchMode === 'general' && error && <ErrorState message={(error as Error).message} />}
        {searchMode === 'general' && !isLoading && !error && documents?.length === 0 && (
          <div className="empty-state">
            <Search size={28} />
            <h3>일치하는 문서가 없습니다</h3>
            <p>다른 검색어나 카테고리를 선택해 보세요.</p>
          </div>
        )}

        {searchMode === 'general' && <div className={`document-grid ${viewMode === 'list' ? 'document-grid--list' : ''}`}>
          {documents?.map((document, index) => (
            <Link className="document-card" to={`/notes/${document.id}`} key={document.id}>
              <div className="document-card__top">
                <span className="document-number">{String(index + 1).padStart(2, '0')}</span>
                <ArrowUpRight size={19} />
              </div>
              <div className="document-card__heading">
                <div className="document-card__icon"><FileText size={20} /></div>
                <h3>{document.title}</h3>
              </div>
              {document.excerpt ? (
                <p className="document-card__excerpt">{document.excerpt}</p>
              ) : (
                <p className="document-card__path">{document.path}</p>
              )}
              {document.tags.length > 0 && (
                <div className="tag-list tag-list--card">
                  {document.tags.slice(0, 3).map((tag) => <span className="tag-chip" key={tag}>#{tag}</span>)}
                </div>
              )}
              <footer>
                <span>{document.category}</span>
                <time dateTime={document.updatedAt}>{formatDate(document.updatedAt)}</time>
              </footer>
            </Link>
          ))}
        </div>}

        {searchMode !== 'general' && !ragSearch.data && !ragSearch.isPending && !ragSearch.error && (
          <div className="semantic-search-guide">
            <BrainCircuit size={30} />
            <h3>{searchMode === 'hybrid' ? '의미와 정확한 키워드를 모두 반영합니다' : searchMode === 'dense' ? '단어가 달라도 의미가 가까운 문서를 찾습니다' : '정확한 기술 용어가 포함된 문서를 찾습니다'}</h3>
            <p>{searchMode === 'sparse' ? '예: “REQUIRES_NEW” 또는 “HTTP 429”' : '예: “REQUIRES_NEW 트랜잭션 전파 방식은?”'}</p>
          </div>
        )}
        {searchMode !== 'general' && ragSearch.isPending && <LoadingState />}
        {searchMode !== 'general' && ragSearch.error && <ErrorState message={(ragSearch.error as Error).message} />}
        {searchMode !== 'general' && ragSearch.data && semanticDocuments.length === 0 && (
          <div className="empty-state">
            <Search size={28} />
            <h3>관련 문서를 찾지 못했습니다</h3>
            <p>{ragSearch.data.rerankingApplied && ragSearch.data.rerankingMinimumScore !== null
              ? `Cohere 관련도 ${(ragSearch.data.rerankingMinimumScore * 100).toFixed(0)}% 이상인 Chunk가 없습니다.`
              : '다른 표현으로 질문하거나 문서 색인 상태를 확인해 보세요.'}</p>
          </div>
        )}
        {searchMode !== 'general' && semanticDocuments.length > 0 && (
          <div className="semantic-results">
            {semanticDocuments.map((document) => (
              <article className="semantic-result-card" key={document.documentId}>
                <header>
                  <div>
                    <span className="semantic-score">{document.reranked ? '재정렬 관련도' : searchMode === 'hybrid' ? '통합 관련도' : '관련도'} {(document.bestScore * 100).toFixed(1)}%</span>
                    <h3>{document.title}</h3>
                    <p>{document.path}</p>
                  </div>
                  <Link to={`/notes/${document.documentId}`} className="semantic-source-link">
                    원문 보기 <ArrowRight size={14} />
                  </Link>
                </header>
                <div className="semantic-hit-list">
                  {document.hits.map((hit) => (
                    <div className="semantic-hit" key={hit.chunkId}>
                      <div className="semantic-hit__meta">
                        <span>{hit.sectionPath.length > 0 ? hit.sectionPath.join(' › ') : '문서 서두'}</span>
                        <small>{searchMode === 'hybrid' ? matchedByLabel(hit) : hit.score.toFixed(3)}</small>
                      </div>
                      <p>{hit.content}</p>
                    </div>
                  ))}
                </div>
                {document.tags.length > 0 && (
                  <div className="tag-list tag-list--semantic">
                    {document.tags.slice(0, 5).map((tag) => <span className="tag-chip" key={tag}>#{tag}</span>)}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
