import { lazy, Suspense, useState } from 'react'
import { BookOpenText, Braces, Menu, Search, X } from 'lucide-react'
import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'

const DocumentListPage = lazy(() => import('./pages/DocumentListPage').then((module) => ({ default: module.DocumentListPage })))
const DocumentPage = lazy(() => import('./pages/DocumentPage').then((module) => ({ default: module.DocumentPage })))
const DocumentEditorPage = lazy(() => import('./pages/DocumentEditorPage').then((module) => ({ default: module.DocumentEditorPage })))
const TrashPage = lazy(() => import('./pages/TrashPage').then((module) => ({ default: module.TrashPage })))
const RagAnswerPage = lazy(() => import('./pages/RagAnswerPage').then((module) => ({ default: module.RagAnswerPage })))
const RagIndexingPage = lazy(() => import('./pages/RagIndexingPage').then((module) => ({ default: module.RagIndexingPage })))

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="app-shell">
      <header className="mobile-header">
        <Link to="/notes" className="mobile-brand" onClick={() => setSidebarOpen(false)}>
          <span className="brand-mark"><Braces size={18} /></span>
          <span>Kernel</span>
        </Link>
        <button
          className="icon-button"
          type="button"
          aria-label={sidebarOpen ? '메뉴 닫기' : '메뉴 열기'}
          onClick={() => setSidebarOpen((open) => !open)}
        >
          {sidebarOpen ? <X size={21} /> : <Menu size={21} />}
        </button>
      </header>

      <aside className={`sidebar ${sidebarOpen ? 'sidebar--open' : ''}`}>
        <div className="brand-block">
          <Link to="/notes" className="brand" onClick={() => setSidebarOpen(false)}>
            <span className="brand-mark"><Braces size={20} /></span>
            <span>
              <strong>Kernel</strong>
              <small>BACKEND CS NOTES</small>
            </span>
          </Link>
          <p>흩어진 지식을 연결하고,<br />나만의 언어로 정리합니다.</p>
        </div>

        <Sidebar onNavigate={() => setSidebarOpen(false)} />

        <div className="sidebar-footer">
          <div className="sidebar-footer__icon"><BookOpenText size={17} /></div>
          <div>
            <span>Knowledge base</span>
            <small>Markdown powered</small>
          </div>
        </div>
      </aside>

      {sidebarOpen && <button className="sidebar-backdrop" aria-label="메뉴 닫기" onClick={() => setSidebarOpen(false)} />}

      <main className="main-content">
        <Suspense fallback={<div className="page"><div className="state-panel"><span className="loader" /><p>화면을 준비하는 중</p></div></div>}>
          <Routes>
            <Route path="/" element={<Navigate to="/notes" replace />} />
            <Route path="/notes" element={<DocumentListPage />} />
            <Route path="/notes/new" element={<DocumentEditorPage />} />
            <Route path="/notes/:documentId/edit" element={<DocumentEditorPage />} />
            <Route path="/notes/:documentId" element={<DocumentPage />} />
            <Route path="/trash" element={<TrashPage />} />
            <Route path="/ask" element={<RagAnswerPage />} />
            <Route path="/indexing" element={<RagIndexingPage />} />
            <Route path="*" element={<Navigate to="/notes" replace />} />
          </Routes>
        </Suspense>
      </main>

      <div className="keyboard-hint" aria-hidden="true">
        <Search size={13} /> <span>문서 검색</span><kbd>/</kbd>
      </div>
    </div>
  )
}
