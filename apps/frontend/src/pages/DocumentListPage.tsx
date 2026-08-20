import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ArrowRight, ArrowUpRight, BrainCircuit, FilePlus2, FileText, ListFilter, Search, Sparkles } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import type { FormEvent } from 'react'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import type { RagSearchHit } from '../types'

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
    hits: RagSearchHit[]
  }>()

  results.forEach((hit) => {
    const existing = grouped.get(hit.documentId)
    if (existing) {
      existing.bestScore = Math.max(existing.bestScore, hit.score)
      existing.hits.push(hit)
      return
    }
    grouped.set(hit.documentId, {
      documentId: hit.documentId,
      title: hit.documentTitle,
      path: hit.documentPath,
      tags: hit.tags,
      bestScore: hit.score,
      hits: [hit],
    })
  })
  return [...grouped.values()]
}

export function DocumentListPage() {
  const [searchParams] = useSearchParams()
  const category = searchParams.get('category') ?? undefined
  const [query, setQuery] = useState('')
  const [searchMode, setSearchMode] = useState<'general' | 'semantic'>('general')
  const debouncedQuery = useDebouncedValue(query, 250)
  const searchInput = useRef<HTMLInputElement>(null)
  const { data: documents, isLoading, error } = useQuery({
    queryKey: ['documents', category, debouncedQuery],
    queryFn: () => api.documents(category, debouncedQuery),
    placeholderData: (previousData) => previousData,
    enabled: searchMode === 'general',
  })
  const semanticSearch = useMutation({ mutationFn: api.semanticSearch })
  const semanticDocuments = groupSemanticResults(semanticSearch.data?.results ?? [])

  const changeSearchMode = (mode: 'general' | 'semantic') => {
    setSearchMode(mode)
    semanticSearch.reset()
  }

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    if (searchMode === 'semantic' && query.trim()) semanticSearch.mutate(query.trim())
  }

  useEffect(() => {
    const focusSearch = (event: KeyboardEvent) => {
      if (event.key === '/' && document.activeElement?.tagName !== 'INPUT') {
        event.preventDefault()
        searchInput.current?.focus()
      }
    }
    window.addEventListener('keydown', focusSearch)
    return () => window.removeEventListener('keydown', focusSearch)
  }, [])

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
            aria-selected={searchMode === 'semantic'}
            className={searchMode === 'semantic' ? 'active' : ''}
            onClick={() => changeSearchMode('semantic')}
          >
            <BrainCircuit size={14} /> 의미 검색
          </button>
        </div>

        <form className={`search-box ${searchMode === 'semantic' ? 'search-box--semantic' : ''}`} onSubmit={submitSearch}>
          {searchMode === 'semantic' ? <BrainCircuit size={20} /> : <Search size={20} />}
          <input
            ref={searchInput}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={searchMode === 'semantic' ? '의미나 개념을 문장으로 질문해 보세요' : '제목, 태그 또는 본문 검색'}
            aria-label={searchMode === 'semantic' ? '의미 검색' : '문서 검색'}
          />
          {searchMode === 'semantic' ? (
            <button className="semantic-search-button" type="submit" disabled={!query.trim() || semanticSearch.isPending}>
              {semanticSearch.isPending ? '검색 중' : '검색'}
            </button>
          ) : <kbd>/</kbd>}
        </form>
        {searchMode === 'semantic' && (
          <p className="semantic-search-note">
            Enter 또는 검색 버튼을 누를 때만 의미 검색을 실행합니다.
            {category ? ' 의미 검색은 선택한 폴더와 관계없이 전체 문서를 대상으로 합니다.' : ''}
          </p>
        )}
      </section>

      <section className="library-section">
        <header className="section-header">
          <div>
            <span>{searchMode === 'semantic' ? 'SEMANTIC RETRIEVAL' : 'COLLECTION'}</span>
            <h2>{searchMode === 'semantic'
              ? semanticSearch.data ? `'${semanticSearch.data.query}' 관련 문서` : '의미로 문서 찾기'
              : debouncedQuery ? `'${debouncedQuery}' 검색 결과` : category ? `${category} 문서` : '전체 문서'}</h2>
          </div>
          <div className="section-actions">
            <p>{searchMode === 'semantic' ? `${semanticDocuments.length} related notes` : `${documents?.length ?? 0} notes`}</p>
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

        {searchMode === 'general' && <div className="document-grid">
          {documents?.map((document, index) => (
            <Link className="document-card" to={`/notes/${document.id}`} key={document.id}>
              <div className="document-card__top">
                <span className="document-number">{String(index + 1).padStart(2, '0')}</span>
                <ArrowUpRight size={19} />
              </div>
              <div className="document-card__icon"><FileText size={21} /></div>
              <h3>{document.title}</h3>
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

        {searchMode === 'semantic' && !semanticSearch.data && !semanticSearch.isPending && !semanticSearch.error && (
          <div className="semantic-search-guide">
            <BrainCircuit size={30} />
            <h3>단어가 달라도 의미가 가까운 문서를 찾습니다</h3>
            <p>예: “트랜잭션이 다른 메서드로 전파되는 방식은?”</p>
          </div>
        )}
        {searchMode === 'semantic' && semanticSearch.isPending && <LoadingState />}
        {searchMode === 'semantic' && semanticSearch.error && <ErrorState message={(semanticSearch.error as Error).message} />}
        {searchMode === 'semantic' && semanticSearch.data && semanticDocuments.length === 0 && (
          <div className="empty-state">
            <Search size={28} />
            <h3>관련 문서를 찾지 못했습니다</h3>
            <p>다른 표현으로 질문하거나 문서 색인 상태를 확인해 보세요.</p>
          </div>
        )}
        {searchMode === 'semantic' && semanticDocuments.length > 0 && (
          <div className="semantic-results">
            {semanticDocuments.map((document) => (
              <article className="semantic-result-card" key={document.documentId}>
                <header>
                  <div>
                    <span className="semantic-score">관련도 {(document.bestScore * 100).toFixed(1)}%</span>
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
                        <small>{hit.score.toFixed(3)}</small>
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
