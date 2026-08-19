import { useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowUpRight, FilePlus2, FileText, Search, Sparkles } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { useDebouncedValue } from '../hooks/useDebouncedValue'

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric' }).format(new Date(value))
}

export function DocumentListPage() {
  const [searchParams] = useSearchParams()
  const category = searchParams.get('category') ?? undefined
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query, 250)
  const searchInput = useRef<HTMLInputElement>(null)
  const { data: documents, isLoading, error } = useQuery({
    queryKey: ['documents', category, debouncedQuery],
    queryFn: () => api.documents(category, debouncedQuery),
    placeholderData: (previousData) => previousData,
  })

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

        <label className="search-box">
          <Search size={20} />
          <input
            ref={searchInput}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="문서 제목 또는 경로 검색"
            aria-label="문서 검색"
          />
          <kbd>/</kbd>
        </label>
      </section>

      <section className="library-section">
        <header className="section-header">
          <div>
            <span>COLLECTION</span>
            <h2>{debouncedQuery ? `'${debouncedQuery}' 검색 결과` : category ? `${category} 문서` : '전체 문서'}</h2>
          </div>
          <div className="section-actions">
            <p>{documents?.length ?? 0} notes</p>
            <Link to="/notes/new" className="primary-button primary-button--small">
              <FilePlus2 size={15} /> 새 문서
            </Link>
          </div>
        </header>

        {isLoading && <LoadingState />}
        {error && <ErrorState message={(error as Error).message} />}
        {!isLoading && !error && documents?.length === 0 && (
          <div className="empty-state">
            <Search size={28} />
            <h3>일치하는 문서가 없습니다</h3>
            <p>다른 검색어나 카테고리를 선택해 보세요.</p>
          </div>
        )}

        <div className="document-grid">
          {documents?.map((document, index) => (
            <Link className="document-card" to={`/notes/${document.id}`} key={document.id}>
              <div className="document-card__top">
                <span className="document-number">{String(index + 1).padStart(2, '0')}</span>
                <ArrowUpRight size={19} />
              </div>
              <div className="document-card__icon"><FileText size={21} /></div>
              <h3>{document.title}</h3>
              <p>{document.path}</p>
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
        </div>
      </section>
    </div>
  )
}
