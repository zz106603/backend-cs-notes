import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Check, Eye, FilePenLine, Save } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { MarkdownTextarea } from '../components/MarkdownTextarea'
import type { Category, DocumentDetail } from '../types'

function flattenCategories(categories: Category[]): Category[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children)])
}

export function DocumentEditorPage() {
  const { documentId } = useParams()
  const isEditing = Boolean(documentId)
  const { data: document, isLoading, error } = useQuery({
    queryKey: ['document', documentId],
    queryFn: () => api.document(documentId!),
    enabled: isEditing,
  })

  if (isEditing && isLoading) return <div className="page page--editor"><LoadingState label="편집할 문서를 불러오는 중" /></div>
  if (error) return <div className="page page--editor"><ErrorState message={(error as Error).message} /></div>
  if (isEditing && !document) return null

  return <DocumentEditorForm key={document?.id ?? 'new'} document={document} />
}

function DocumentEditorForm({ document }: { document?: DocumentDetail }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: api.categories })
  const categoryOptions = useMemo(() => flattenCategories(categories ?? []), [categories])
  const initialValues = useMemo(() => ({
    title: document?.title ?? '',
    category: document?.category ?? '',
    content: document?.content ?? '# 새 문서\n\n여기에 학습한 내용을 정리해 보세요.\n',
    tags: document?.tags.join(', ') ?? '',
  }), [document])
  const [title, setTitle] = useState(initialValues.title)
  const [category, setCategory] = useState(initialValues.category)
  const [content, setContent] = useState(initialValues.content)
  const [tags, setTags] = useState(initialValues.tags)
  const [mode, setMode] = useState<'split' | 'write' | 'preview'>('split')

  const parsedTags = useMemo(() => [...new Set(tags.split(',').map((tag) => tag.trim()).filter(Boolean))], [tags])
  const isDirty = !document || title !== initialValues.title || category !== initialValues.category || content !== initialValues.content || tags !== initialValues.tags
  const isValid = title.trim().length > 0 && category.trim().length > 0 && content.length <= 1_000_000 && parsedTags.length <= 10 && parsedTags.every((tag) => tag.length <= 30)
  const previewContent = useMemo(() => {
    if (!title.trim()) return content
    const headingPattern = /^# .+$/m
    return headingPattern.test(content)
      ? content.replace(headingPattern, `# ${title.trim()}`)
      : `# ${title.trim()}\n\n${content}`
  }, [content, title])

  useEffect(() => {
    const preventAccidentalClose = (event: BeforeUnloadEvent) => {
      if (!isDirty) return
      event.preventDefault()
    }
    window.addEventListener('beforeunload', preventAccidentalClose)
    return () => window.removeEventListener('beforeunload', preventAccidentalClose)
  }, [isDirty])

  const saveMutation = useMutation({
    mutationFn: () => document
      ? api.updateDocument(document.id, {
          title: title.trim(),
          category: category.trim(),
          content,
          tags: parsedTags,
          expectedUpdatedAt: document.updatedAt,
        })
      : api.createDocument({ title: title.trim(), category: category.trim(), content, tags: parsedTags }),
    onSuccess: async (savedDocument) => {
      queryClient.setQueryData(['document', savedDocument.id], savedDocument)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['documents'] }),
        queryClient.invalidateQueries({ queryKey: ['categories'] }),
      ])
      navigate(`/notes/${savedDocument.id}`, { replace: true })
    },
  })

  const leaveEditor = () => {
    if (isDirty && !window.confirm('저장하지 않은 변경사항이 있습니다. 편집을 종료할까요?')) return
    navigate(document ? `/notes/${document.id}` : '/notes')
  }

  return (
    <div className="page page--editor">
      <header className="editor-topbar">
        <button type="button" className="back-link back-link--button" onClick={leaveEditor}>
          <ArrowLeft size={16} /> 편집 종료
        </button>
        <div className="editor-status">
          {saveMutation.isSuccess ? <><Check size={14} /> 저장 완료</> : isDirty ? '저장하지 않은 변경사항' : '모든 변경사항 저장됨'}
        </div>
        <button
          type="button"
          className="primary-button"
          onClick={() => saveMutation.mutate()}
          disabled={!isValid || saveMutation.isPending}
        >
          <Save size={16} /> {saveMutation.isPending ? '저장 중...' : '문서 저장'}
        </button>
      </header>

      <section className="editor-heading">
        <div className="document-kicker"><FilePenLine size={15} /> {document ? 'EDIT KNOWLEDGE NOTE' : 'NEW KNOWLEDGE NOTE'}</div>
        <h1>{document ? '문서 다듬기' : '새로운 지식 기록하기'}</h1>
        <p>제목과 카테고리를 정하고 Markdown으로 내용을 작성하세요.</p>
      </section>

      <section className="editor-metadata">
        <label>
          <span>문서 제목</span>
          <input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={100} placeholder="예: 트랜잭션 격리 수준" />
        </label>
        <label>
          <span>카테고리</span>
          <input
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            maxLength={100}
            placeholder="예: 데이터베이스"
            list="category-options"
          />
          <datalist id="category-options">
            {categoryOptions.map((item) => <option value={item.path} key={item.path} />)}
          </datalist>
        </label>
        <label className="editor-metadata__tags">
          <span>태그 · 쉼표로 구분 (최대 10개)</span>
          <input value={tags} onChange={(event) => setTags(event.target.value)} placeholder="예: Spring, JPA, 트랜잭션" />
        </label>
      </section>

      {saveMutation.error && <div className="editor-error" role="alert">{(saveMutation.error as Error).message}</div>}

      <section className="editor-workspace">
        <header className="editor-toolbar">
          <div>
            <span className="editor-file-dot" />
            <strong>{title.trim() || '새 문서'}.md</strong>
          </div>
          <div className="editor-mode" role="group" aria-label="편집 화면 모드">
            <button className={mode === 'write' ? 'active' : ''} onClick={() => setMode('write')} type="button">작성</button>
            <button className={mode === 'split' ? 'active' : ''} onClick={() => setMode('split')} type="button">분할</button>
            <button className={mode === 'preview' ? 'active' : ''} onClick={() => setMode('preview')} type="button"><Eye size={14} /> 미리보기</button>
          </div>
        </header>

        <div className={`editor-panes editor-panes--${mode}`}>
          <MarkdownTextarea value={content} visible={mode !== 'preview'} onChange={setContent} />
          {mode !== 'write' && (
            <div className="editor-preview-pane">
              <div className="markdown-body">
                <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
                  {previewContent || '*미리보기할 내용을 입력하세요.*'}
                </ReactMarkdown>
              </div>
            </div>
          )}
        </div>
        <footer className="editor-footer">
          <span>Markdown</span>
          <span>{content.length.toLocaleString()}자</span>
        </footer>
      </section>

      <div className="editor-mobile-save">
        <button type="button" className="primary-button" onClick={() => saveMutation.mutate()} disabled={!isValid || saveMutation.isPending}>
          <Save size={16} /> {saveMutation.isPending ? '저장 중...' : '문서 저장'}
        </button>
      </div>
    </div>
  )
}
