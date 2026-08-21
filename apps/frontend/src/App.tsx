import { lazy, Suspense, useEffect, useState } from 'react'
import { BookOpenText, Braces, Menu, Moon, PanelLeftClose, PanelLeftOpen, Search, Sun, X } from 'lucide-react'
import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { GlobalSearchModal } from './components/GlobalSearchModal'
import { usePersistentState } from './hooks/usePersistentState'

const DocumentListPage = lazy(() => import('./pages/DocumentListPage').then((module) => ({ default: module.DocumentListPage })))
const DocumentPage = lazy(() => import('./pages/DocumentPage').then((module) => ({ default: module.DocumentPage })))
const DocumentEditorPage = lazy(() => import('./pages/DocumentEditorPage').then((module) => ({ default: module.DocumentEditorPage })))
const TrashPage = lazy(() => import('./pages/TrashPage').then((module) => ({ default: module.TrashPage })))
const RagAnswerPage = lazy(() => import('./pages/RagAnswerPage').then((module) => ({ default: module.RagAnswerPage })))
const RagIndexingPage = lazy(() => import('./pages/RagIndexingPage').then((module) => ({ default: module.RagIndexingPage })))

export default function App() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = usePersistentState('cs-notes-sidebar-collapsed', false)
  const preferredTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  const [theme, setTheme] = usePersistentState<'light' | 'dark'>('cs-notes-theme', preferredTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
  }, [theme])

  useEffect(() => {
    const openSearch = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null
      const isEditing = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA' || target?.isContentEditable
      if (event.key === '/' && !isEditing) {
        event.preventDefault()
        setSidebarOpen(false)
        setSearchOpen(true)
      }
    }
    window.addEventListener('keydown', openSearch)
    return () => window.removeEventListener('keydown', openSearch)
  }, [])

  return (
    <div className={`app-shell ${sidebarCollapsed ? 'app-shell--sidebar-collapsed' : ''}`}>
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
        <button
          className="sidebar-collapse-button"
          type="button"
          aria-label="사이드바 숨기기"
          title="사이드바 숨기기"
          onClick={() => setSidebarCollapsed(true)}
        >
          <PanelLeftClose size={17} />
        </button>
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

        <Sidebar
          onNavigate={() => setSidebarOpen(false)}
          onOpenSearch={() => {
            setSidebarOpen(false)
            setSearchOpen(true)
          }}
        />

        <div className="sidebar-footer">
          <div className="sidebar-footer__icon"><BookOpenText size={17} /></div>
          <div>
            <span>Knowledge base</span>
            <small>Markdown powered</small>
          </div>
          <button
            className="theme-toggle"
            type="button"
            aria-label={theme === 'light' ? '다크 테마로 변경' : '라이트 테마로 변경'}
            title={theme === 'light' ? '다크 테마' : '라이트 테마'}
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
          >
            {theme === 'light' ? <Moon size={16} /> : <Sun size={16} />}
          </button>
        </div>
      </aside>

      {sidebarOpen && <button className="sidebar-backdrop" aria-label="메뉴 닫기" onClick={() => setSidebarOpen(false)} />}

      {sidebarCollapsed && (
        <button
          className="sidebar-expand-button"
          type="button"
          aria-label="사이드바 열기"
          title="사이드바 열기"
          onClick={() => setSidebarCollapsed(false)}
        >
          <PanelLeftOpen size={18} />
        </button>
      )}

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

      {searchOpen && <GlobalSearchModal onClose={() => setSearchOpen(false)} />}
    </div>
  )
}
