import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarDays, FileText, FolderOpen, Pencil, Trash2 } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
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
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: document, isLoading, error } = useQuery({
    queryKey: ['document', documentId],
    queryFn: () => api.document(documentId),
    enabled: Boolean(documentId),
  })
  const trashMutation = useMutation({
    mutationFn: () => api.moveDocumentToTrash(documentId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['documents'] }),
        queryClient.invalidateQueries({ queryKey: ['categories'] }),
      ])
      navigate('/notes', { replace: true })
    },
  })

  if (isLoading) return <div className="page page--document"><LoadingState /></div>
  if (error) return <div className="page page--document"><ErrorState message={(error as Error).message} /></div>
  if (!document) return null

  return (
    <div className="page page--document">
      <div className="document-actions">
        <Link to={`/notes?category=${encodeURIComponent(document.category)}`} className="back-link">
          <ArrowLeft size={16} /> {document.category} 목록으로
        </Link>
        <div>
          <Link to={`/notes/${document.id}/edit`} className="secondary-button"><Pencil size={15} /> 편집</Link>
          <button
            type="button"
            className="secondary-button secondary-button--danger"
            disabled={trashMutation.isPending}
            onClick={() => {
              if (window.confirm('이 문서를 휴지통으로 이동할까요?')) trashMutation.mutate()
            }}
          >
            <Trash2 size={15} /> {trashMutation.isPending ? '이동 중...' : '휴지통'}
          </button>
        </div>
      </div>

      {trashMutation.error && <div className="editor-error" role="alert">{(trashMutation.error as Error).message}</div>}

      <article className="document-view">
        <header className="document-header">
          <div className="document-kicker"><FileText size={15} /> KNOWLEDGE NOTE</div>
          <h1>{document.title}</h1>
          <div className="document-meta">
            <span><FolderOpen size={15} /> {document.path}</span>
            <span><CalendarDays size={15} /> {formatDate(document.updatedAt)} 수정</span>
          </div>
          {document.tags.length > 0 && (
            <div className="tag-list" aria-label="문서 태그">
              {document.tags.map((tag) => <span className="tag-chip" key={tag}>#{tag}</span>)}
            </div>
          )}
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
