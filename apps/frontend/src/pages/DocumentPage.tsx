import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, BookOpenText, CalendarDays, FileText, FolderOpen, Pencil, Trash2, Type } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import type { Components } from 'react-markdown'
import { isValidElement } from 'react'
import type { ReactElement, ReactNode } from 'react'
import rehypeHighlight from 'rehype-highlight'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { usePersistentState } from '../hooks/usePersistentState'

type ReadingSize = 'small' | 'default' | 'large'
type ReadingWidth = 'compact' | 'default' | 'wide'
type ReadingFont = 'sans' | 'serif'

interface ReadingPreferences {
  size: ReadingSize
  width: ReadingWidth
  font: ReadingFont
}

interface TableOfContentsItem {
  level: 2 | 3
  text: string
  id: string
}

function plainText(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') return String(node)
  if (Array.isArray(node)) return node.map(plainText).join('')
  if (isValidElement(node)) return plainText((node as ReactElement<{ children?: ReactNode }>).props.children)
  return ''
}

function headingSlug(text: string) {
  return text.normalize('NFKC').toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '') || 'section'
}

function uniqueHeadingId(text: string, occurrences: Map<string, number>) {
  const slug = headingSlug(text)
  const count = occurrences.get(slug) ?? 0
  occurrences.set(slug, count + 1)
  return count === 0 ? slug : `${slug}-${count + 1}`
}

function extractTableOfContents(markdown: string): TableOfContentsItem[] {
  const occurrences = new Map<string, number>()
  let fenced = false
  const items: TableOfContentsItem[] = []
  markdown.split(/\r?\n/).forEach((line) => {
    if (/^\s*(```|~~~)/.test(line)) {
      fenced = !fenced
      return
    }
    if (fenced) return
    const match = /^(#{2,3})\s+(.+?)\s*#*\s*$/.exec(line)
    if (!match) return
    const text = match[2]
      .replace(/!\[([^\]]*)]\([^)]*\)/g, '$1')
      .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
      .replace(/[*_`~]/g, '')
      .trim()
    items.push({ level: match[1].length as 2 | 3, text, id: uniqueHeadingId(text, occurrences) })
  })
  return items
}

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
  const [reading, setReading] = usePersistentState<ReadingPreferences>('cs-notes-reading', {
    size: 'default', width: 'default', font: 'sans',
  })
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

  const tableOfContents = extractTableOfContents(document.content)
  const renderedHeadingOccurrences = new Map<string, number>()
  const heading = (level: 2 | 3) => ({ children, node, ...props }: React.HTMLAttributes<HTMLHeadingElement> & { node?: unknown }) => {
    void node
    const text = plainText(children)
    const id = uniqueHeadingId(text, renderedHeadingOccurrences)
    const Heading = `h${level}` as 'h2' | 'h3'
    return <Heading id={id} {...props}>{children}</Heading>
  }
  const markdownComponents: Components = { h2: heading(2), h3: heading(3) }
  const readingClass = `reading-size--${reading.size} reading-width--${reading.width} reading-font--${reading.font}`

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

      <section className="reading-controls" aria-label="읽기 설정">
        <div className="reading-controls__title"><Type size={16} /><span>읽기 설정</span></div>
        <div className="reading-control-group">
          <span>글자</span>
          {(['small', 'default', 'large'] as const).map((size) => (
            <button type="button" className={reading.size === size ? 'active' : ''} aria-pressed={reading.size === size} onClick={() => setReading({ ...reading, size })} key={size}>
              {size === 'small' ? '작게' : size === 'default' ? '기본' : '크게'}
            </button>
          ))}
        </div>
        <div className="reading-control-group">
          <span>본문 폭</span>
          {(['compact', 'default', 'wide'] as const).map((width) => (
            <button type="button" className={reading.width === width ? 'active' : ''} aria-pressed={reading.width === width} onClick={() => setReading({ ...reading, width })} key={width}>
              {width === 'compact' ? '좁게' : width === 'default' ? '기본' : '넓게'}
            </button>
          ))}
        </div>
        <div className="reading-control-group">
          <span>글꼴</span>
          {(['sans', 'serif'] as const).map((font) => (
            <button type="button" className={reading.font === font ? 'active' : ''} aria-pressed={reading.font === font} onClick={() => setReading({ ...reading, font })} key={font}>
              {font === 'sans' ? '고딕' : '명조'}
            </button>
          ))}
        </div>
      </section>

      <article className={`document-view ${readingClass}`}>
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

        <div className="document-reading-layout">
          <div className="markdown-body">
            <ReactMarkdown components={markdownComponents} remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
              {document.content}
            </ReactMarkdown>
          </div>
          {tableOfContents.length > 0 && (
            <aside className="document-toc">
              <header><BookOpenText size={15} /><span>문서 목차</span></header>
              <nav aria-label="문서 목차">
                {tableOfContents.map((item) => (
                  <a className={item.level === 3 ? 'document-toc__item document-toc__item--nested' : 'document-toc__item'} href={`#${item.id}`} key={item.id}>{item.text}</a>
                ))}
              </nav>
            </aside>
          )}
        </div>
      </article>
    </div>
  )
}
