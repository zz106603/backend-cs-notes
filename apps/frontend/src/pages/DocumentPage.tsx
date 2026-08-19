import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, CalendarDays, FileText, FolderOpen } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(new Date(value))
}

export function DocumentPage() {
  const { documentId = '' } = useParams()
  const { data: document, isLoading, error } = useQuery({
    queryKey: ['document', documentId],
    queryFn: () => api.document(documentId),
    enabled: Boolean(documentId),
  })

  if (isLoading) return <div className="page page--document"><LoadingState /></div>
  if (error) return <div className="page page--document"><ErrorState message={(error as Error).message} /></div>
  if (!document) return null

  return (
    <div className="page page--document">
      <Link to={`/notes?category=${encodeURIComponent(document.category)}`} className="back-link">
        <ArrowLeft size={16} /> {document.category} 목록으로
      </Link>

      <article className="document-view">
        <header className="document-header">
          <div className="document-kicker"><FileText size={15} /> KNOWLEDGE NOTE</div>
          <h1>{document.title}</h1>
          <div className="document-meta">
            <span><FolderOpen size={15} /> {document.path}</span>
            <span><CalendarDays size={15} /> {formatDate(document.updatedAt)} 수정</span>
          </div>
        </header>

        <div className="document-divider"><span /></div>

        <div className="markdown-body">
          <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
            {document.content}
          </ReactMarkdown>
        </div>
      </article>
    </div>
  )
}
