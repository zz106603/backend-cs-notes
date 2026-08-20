import { useMutation } from '@tanstack/react-query'
import { ArrowRight, BookMarked, BrainCircuit, CircleDollarSign, Send, Sparkles } from 'lucide-react'
import type { FormEvent } from 'react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import { ErrorState } from '../components/ErrorState'

export function RagAnswerPage() {
  const [question, setQuestion] = useState('')
  const answer = useMutation({ mutationFn: api.ragAnswer })

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (question.trim() && !answer.isPending) answer.mutate(question.trim())
  }

  return (
    <div className="page page--rag-answer">
      <section className="rag-answer-hero">
        <div className="eyebrow"><Sparkles size={14} /> GROUNDED STUDY ASSISTANT</div>
        <h1>내 문서에 질문하기</h1>
        <p>저장한 CS 문서에서 관련 내용을 찾고, 검색된 근거만 사용해 학습 답변을 만듭니다.</p>

        <form className="rag-question-form" onSubmit={submit}>
          <label htmlFor="rag-question">질문</label>
          <textarea
            id="rag-question"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="예: Spring 트랜잭션 전파는 어떤 상황에서 사용하나요?"
            maxLength={500}
            rows={4}
          />
          <footer>
            <span><CircleDollarSign size={14} /> 질문마다 검색 및 답변 API 비용이 발생할 수 있습니다.</span>
            <button className="primary-button" type="submit" disabled={!question.trim() || answer.isPending}>
              <Send size={14} /> {answer.isPending ? '답변 생성 중' : '질문하기'}
            </button>
          </footer>
        </form>
      </section>

      {answer.error && <ErrorState message={(answer.error as Error).message} />}

      {!answer.data && !answer.error && !answer.isPending && (
        <section className="rag-answer-empty">
          <BrainCircuit size={32} />
          <h2>문서에 있는 내용부터 확인합니다</h2>
          <p>관련 Chunk가 없으면 OpenAI 답변 생성을 실행하지 않습니다.</p>
        </section>
      )}

      {answer.isPending && (
        <section className="rag-answer-loading">
          <span className="loader" />
          <div><strong>관련 문서를 찾고 있습니다</strong><p>근거를 구성한 뒤 답변을 생성합니다.</p></div>
        </section>
      )}

      {answer.data && (
        <div className="rag-answer-layout">
          <article className="rag-answer-panel">
            <header>
              <div><BrainCircuit size={18} /><span>{answer.data.answerModel}</span></div>
              <div className="rag-answer-badges">
                {!answer.data.generated && <span>근거 없음</span>}
                {answer.data.cached && <span>캐시 응답</span>}
              </div>
            </header>
            <div className="markdown-body rag-answer-content">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>{answer.data.answer}</ReactMarkdown>
            </div>
            <footer>
              <span>Context {answer.data.contextCharacters.toLocaleString()} chars</span>
              {answer.data.usage.totalTokens != null && (
                <span>Total {answer.data.usage.totalTokens.toLocaleString()} tokens</span>
              )}
            </footer>
          </article>

          <aside className="rag-source-panel">
            <header><BookMarked size={17} /><div><span>SOURCES</span><strong>참고 문서 {answer.data.sources.length}</strong></div></header>
            {answer.data.sources.length === 0 ? (
              <p className="rag-source-empty">질문과 관련된 문서 근거를 찾지 못했습니다.</p>
            ) : (
              <div className="rag-source-list">
                {answer.data.sources.map((source) => (
                  <Link className="rag-source-card" to={`/notes/${source.documentId}`} key={source.number}>
                    <div><span>[{source.number}]</span><small>{source.score.toFixed(3)}</small></div>
                    <strong>{source.documentTitle}</strong>
                    <p>{source.sectionPath.length > 0 ? source.sectionPath.join(' › ') : '문서 서두'}</p>
                    <footer>원문 보기 <ArrowRight size={13} /></footer>
                  </Link>
                ))}
              </div>
            )}
          </aside>
        </div>
      )}
    </div>
  )
}
