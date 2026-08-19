import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FileText, RotateCcw, ShieldAlert, Trash2, X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'

function formatDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export function TrashPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { data: documents, isLoading, error } = useQuery({
    queryKey: ['trash'],
    queryFn: api.trashDocuments,
  })
  const deleteMutation = useMutation({
    mutationFn: api.permanentlyDeleteTrashDocument,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['trash'] }),
  })
  const restoreMutation = useMutation({
    mutationFn: api.restoreTrashDocument,
    onSuccess: async (restoredDocument) => {
      queryClient.setQueryData(['document', restoredDocument.id], restoredDocument)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trash'] }),
        queryClient.invalidateQueries({ queryKey: ['documents'] }),
        queryClient.invalidateQueries({ queryKey: ['categories'] }),
      ])
      navigate(`/notes/${restoredDocument.id}`)
    },
  })
  const mutationError = restoreMutation.error ?? deleteMutation.error

  return (
    <div className="page page--trash">
      <section className="trash-hero">
        <div className="eyebrow"><Trash2 size={14} /> DOCUMENT TRASH</div>
        <h1>휴지통</h1>
        <p>삭제한 문서는 일반 목록에서 숨겨집니다. 여기에서 영구 삭제하면 다시 되돌릴 수 없습니다.</p>
      </section>

      <div className="trash-warning">
        <ShieldAlert size={18} />
        <span><strong>영구 삭제 주의</strong> 휴지통에서 삭제한 파일은 로컬 파일 시스템에서 제거됩니다.</span>
      </div>

      {mutationError && <div className="editor-error" role="alert">{(mutationError as Error).message}</div>}

      <section className="trash-section">
        <header className="section-header">
          <div>
            <span>DELETED NOTES</span>
            <h2>삭제된 문서</h2>
          </div>
          <p>{documents?.length ?? 0} notes</p>
        </header>

        {isLoading && <LoadingState label="휴지통을 확인하는 중" />}
        {error && <ErrorState message={(error as Error).message} />}
        {!isLoading && !error && documents?.length === 0 && (
          <div className="empty-state trash-empty">
            <Trash2 size={29} />
            <h3>휴지통이 비어 있습니다</h3>
            <p>삭제한 문서가 이곳에 표시됩니다.</p>
          </div>
        )}

        <div className="trash-list">
          {documents?.map((document) => (
            <article className="trash-item" key={document.id}>
              <div className="trash-item__icon"><FileText size={19} /></div>
              <div className="trash-item__content">
                <h3>{document.title}</h3>
                <p>{document.originalPath}</p>
              </div>
              <time dateTime={document.deletedAt}>{formatDate(document.deletedAt)}</time>
              <div className="trash-item__actions">
                <button
                  type="button"
                  className="restore-button"
                  disabled={restoreMutation.isPending || deleteMutation.isPending}
                  onClick={() => restoreMutation.mutate(document.id)}
                >
                  <RotateCcw size={14} /> {restoreMutation.isPending && restoreMutation.variables === document.id ? '복원 중...' : '복원'}
                </button>
                <button
                  type="button"
                  className="permanent-delete-button"
                  disabled={restoreMutation.isPending || deleteMutation.isPending}
                  onClick={() => {
                    if (window.confirm(`'${document.title}' 문서를 영구 삭제할까요? 이 작업은 되돌릴 수 없습니다.`)) {
                      deleteMutation.mutate(document.id)
                    }
                  }}
                >
                  <X size={15} /> {deleteMutation.isPending && deleteMutation.variables === document.id ? '삭제 중...' : '영구 삭제'}
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
