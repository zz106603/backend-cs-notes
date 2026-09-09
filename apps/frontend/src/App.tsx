import { lazy, Suspense, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Braces, LogIn, Menu, Moon, PanelLeftClose, PanelLeftOpen, Search, Sun, X } from 'lucide-react'
import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { GlobalSearchModal } from './components/GlobalSearchModal'
import { usePersistentState } from './hooks/usePersistentState'
import { api } from './api'

const DocumentListPage = lazy(() => import('./pages/DocumentListPage').then((module) => ({ default: module.DocumentListPage })))
const DocumentPage = lazy(() => import('./pages/DocumentPage').then((module) => ({ default: module.DocumentPage })))
const DocumentEditorPage = lazy(() => import('./pages/DocumentEditorPage').then((module) => ({ default: module.DocumentEditorPage })))
const TrashPage = lazy(() => import('./pages/TrashPage').then((module) => ({ default: module.TrashPage })))
const RagAnswerPage = lazy(() => import('./pages/RagAnswerPage').then((module) => ({ default: module.RagAnswerPage })))
const RagIndexingPage = lazy(() => import('./pages/RagIndexingPage').then((module) => ({ default: module.RagIndexingPage })))
const RagEvaluationPage = lazy(() => import('./pages/RagEvaluationPage').then((module) => ({ default: module.RagEvaluationPage })))

export default function App() {
  const queryClient = useQueryClient()
  const auth = useQuery({ queryKey: ['auth'], queryFn: api.authStatus, retry: false })
  const logout = useMutation({
    mutationFn: api.logout,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['auth'] })
    },
  })
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

  if (auth.isLoading) {
    return <div className="auth-page"><span className="loader" /><p>로그인 상태를 확인하는 중</p></div>
  }

  if (auth.isError) {
    return <div className="auth-page"><h1>서버에 연결할 수 없습니다.</h1><p>{(auth.error as Error).message}</p></div>
  }

  if (auth.data?.securityEnabled && !auth.data.authenticated) {
    return <div className="auth-page">
      <div className="auth-card">
        <span className="brand-mark"><Braces size={24} /></span>
        <p className="eyebrow">BACKEND CS NOTES</p>
        <h1>Kernel에 로그인</h1>
        <p>내 문서와 검색 기록을 안전하게 사용하려면 Google 계정으로 로그인하세요.</p>
        {new URLSearchParams(window.location.search).get('loginError') && <div className="editor-error">로그인에 실패했습니다. 다시 시도해 주세요.</div>}
        <a className="auth-login-button" href="/oauth2/authorization/google"><LogIn size={17} /> Google로 계속하기</a>
      </div>
    </div>
  }

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
          {auth.data?.user?.pictureUrl
            ? <img className="sidebar-user-avatar" src={auth.data.user.pictureUrl} alt="" referrerPolicy="no-referrer" />
            : <div className="sidebar-user-avatar sidebar-user-avatar--fallback">{auth.data?.user?.displayName?.slice(0, 1) ?? 'K'}</div>}
          <div>
            <span>{auth.data?.user?.displayName ?? 'Local mode'}</span>
            <small>{auth.data?.user?.email ?? 'Authentication disabled'}</small>
          </div>
          {auth.data?.securityEnabled && <button className="sidebar-logout" type="button" onClick={() => logout.mutate()} disabled={logout.isPending}>로그아웃</button>}
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
            <Route path="/evaluation" element={<RagEvaluationPage />} />
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
