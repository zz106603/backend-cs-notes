import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Archive, Boxes, ChevronDown, ChevronRight, Database, FilePlus2, FlaskConical, FolderCog, FolderClosed, MessageCircleQuestion, Network, Plus, RefreshCw, Search, ServerCog, SquareCode, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import type { CSSProperties, FormEvent, ReactNode } from 'react'
import { NavLink, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { usePersistentState } from '../hooks/usePersistentState'
import type { Category } from '../types'

const CATEGORY_ICONS: Record<string, ReactNode> = {
  백엔드: <ServerCog size={17} />,
  데이터베이스: <Database size={17} />,
  네트워크: <Network size={17} />,
  프로그래밍: <SquareCode size={17} />,
  Archive: <Archive size={17} />,
}

export function Sidebar({ onNavigate, onOpenSearch }: { onNavigate: () => void; onOpenSearch: () => void }) {
  const { data: categories, isLoading } = useQuery({ queryKey: ['categories'], queryFn: api.categories })
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const selectedCategory = searchParams.get('category')
  const [collapsedCategories, setCollapsedCategories] = usePersistentState<string[]>('cs-notes-collapsed-categories', [])
  const [folderManagerOpen, setFolderManagerOpen] = useState(false)

  const toggleCategory = (path: string) => {
    setCollapsedCategories(collapsedCategories.includes(path)
      ? collapsedCategories.filter((collapsedPath) => collapsedPath !== path)
      : [...collapsedCategories, path])
  }

  const categoryRenamed = (path: string, newPath: string) => {
    setCollapsedCategories(collapsedCategories.map((collapsedPath) =>
      collapsedPath === path || collapsedPath.startsWith(`${path}/`)
        ? `${newPath}${collapsedPath.slice(path.length)}`
        : collapsedPath))
    if (selectedCategory === path || selectedCategory?.startsWith(`${path}/`)) {
      navigate(`/notes?category=${encodeURIComponent(`${newPath}${selectedCategory.slice(path.length)}`)}`, { replace: true })
    }
  }

  return (
    <nav className="sidebar-nav" aria-label="문서 카테고리">
      <div className="sidebar-actions">
        <NavLink to="/notes/new" className="sidebar-create-button" onClick={onNavigate}>
          <FilePlus2 size={16} />
          <span>새 문서</span>
        </NavLink>
        <button className="sidebar-search-button" type="button" aria-haspopup="dialog" onClick={onOpenSearch}>
          <Search size={16} />
          <span>문서 검색</span>
          <kbd>/</kbd>
        </button>
      </div>

      <span className="nav-label">LIBRARY</span>
      <NavLink
        to="/notes"
        end
        className={({ isActive }) => `nav-item ${isActive && !selectedCategory ? 'nav-item--active' : ''}`}
        onClick={onNavigate}
      >
        <span className="nav-item__icon"><Boxes size={17} /></span>
        <span>전체 문서</span>
        <ChevronRight className="nav-item__chevron" size={15} />
      </NavLink>

      <button className="sidebar-folder-button" type="button" onClick={() => setFolderManagerOpen(true)}>
        <FolderCog size={15} /><span>폴더 관리</span>
      </button>

      {isLoading && <div className="nav-loading">폴더 구조를 불러오는 중...</div>}
      <div className="category-tree">
        {categories?.map((category) => (
          <CategoryTreeItem
            category={category}
            depth={0}
            selectedCategory={selectedCategory}
            collapsedCategories={collapsedCategories}
            onToggle={toggleCategory}
            onNavigate={onNavigate}
            key={category.path}
          />
        ))}
      </div>

      <span className="nav-label nav-label--manage">AI STUDY</span>
      <NavLink
        to="/ask"
        className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`}
        onClick={onNavigate}
      >
        <span className="nav-item__icon"><MessageCircleQuestion size={17} /></span>
        <span>문서에 질문</span>
        <ChevronRight className="nav-item__chevron" size={15} />
      </NavLink>
      <NavLink
        to="/evaluation"
        className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`}
        onClick={onNavigate}
      >
        <span className="nav-item__icon"><FlaskConical size={17} /></span>
        <span>검색 품질 평가</span>
        <ChevronRight className="nav-item__chevron" size={15} />
      </NavLink>

      <span className="nav-label nav-label--manage">MANAGE</span>
      <NavLink
        to="/indexing"
        className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`}
        onClick={onNavigate}
      >
        <span className="nav-item__icon"><RefreshCw size={17} /></span>
        <span>문서 색인</span>
        <ChevronRight className="nav-item__chevron" size={15} />
      </NavLink>
      <NavLink
        to="/trash"
        className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`}
        onClick={onNavigate}
      >
        <span className="nav-item__icon"><Trash2 size={17} /></span>
        <span>휴지통</span>
        <ChevronRight className="nav-item__chevron" size={15} />
      </NavLink>

      {folderManagerOpen && <CategoryManagementModal
        categories={categories ?? []}
        onClose={() => setFolderManagerOpen(false)}
        onRenamed={categoryRenamed}
      />}
    </nav>
  )
}

function flattenCategories(categories: Category[]): Category[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children)])
}

function CategoryManagementModal({
  categories,
  onClose,
  onRenamed,
}: {
  categories: Category[]
  onClose: () => void
  onRenamed: (path: string, newPath: string) => void
}) {
  const queryClient = useQueryClient()
  const flatCategories = useMemo(() => flattenCategories(categories), [categories])
  const [createPath, setCreatePath] = useState('')
  const [selectedPath, setSelectedPath] = useState(flatCategories[0]?.path ?? '')
  const [newPath, setNewPath] = useState(flatCategories[0]?.path ?? '')

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  useEffect(() => {
    if (!selectedPath && flatCategories.length > 0) {
      setSelectedPath(flatCategories[0].path)
      setNewPath(flatCategories[0].path)
    }
  }, [flatCategories, selectedPath])

  const refresh = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['categories'] }),
      queryClient.invalidateQueries({ queryKey: ['documents'] }),
    ])
  }
  const create = useMutation({
    mutationFn: () => api.createCategory(createPath.trim()),
    onSuccess: async () => {
      setCreatePath('')
      await refresh()
    },
  })
  const update = useMutation({
    mutationFn: () => api.updateCategory(selectedPath, newPath.trim()),
    onSuccess: async () => {
      const previousPath = selectedPath
      const updatedPath = newPath.trim()
      setSelectedPath(updatedPath)
      onRenamed(previousPath, updatedPath)
      await refresh()
    },
  })
  const error = create.error ?? update.error
  const submitCreate = (event: FormEvent) => {
    event.preventDefault()
    if (createPath.trim()) create.mutate()
  }
  const submitUpdate = (event: FormEvent) => {
    event.preventDefault()
    if (selectedPath && newPath.trim() && selectedPath !== newPath.trim()) update.mutate()
  }

  return <div className="management-backdrop" role="presentation" onMouseDown={(event) => {
    if (event.target === event.currentTarget) onClose()
  }}>
    <section className="management-modal" role="dialog" aria-modal="true" aria-labelledby="folder-management-title">
      <header>
        <div><span>LIBRARY STRUCTURE</span><h2 id="folder-management-title">폴더 관리</h2></div>
        <button type="button" aria-label="폴더 관리 닫기" onClick={onClose}><X size={17} /></button>
      </header>
      <p className="management-description">경로는 <code>상위/하위</code> 형식으로 입력합니다. 폴더를 수정하면 하위 문서도 함께 이동합니다.</p>

      <form className="management-form" onSubmit={submitCreate}>
        <label><span>새 폴더 경로</span><input value={createPath} onChange={(event) => setCreatePath(event.target.value)} maxLength={100} placeholder="예: 백엔드/Spring" autoFocus /></label>
        <button className="primary-button" type="submit" disabled={!createPath.trim() || create.isPending}><Plus size={14} /> {create.isPending ? '생성 중' : '폴더 추가'}</button>
      </form>

      <form className="management-form management-form--rename" onSubmit={submitUpdate}>
        <label><span>수정할 폴더</span><select value={selectedPath} onChange={(event) => {
          setSelectedPath(event.target.value)
          setNewPath(event.target.value)
        }}>
          {flatCategories.map((category) => <option value={category.path} key={category.path}>{category.path}</option>)}
        </select></label>
        <label><span>새 경로</span><input value={newPath} onChange={(event) => setNewPath(event.target.value)} maxLength={100} placeholder="예: 백엔드/Framework/Spring" /></label>
        <button className="secondary-button" type="submit" disabled={!selectedPath || !newPath.trim() || selectedPath === newPath.trim() || update.isPending}>
          <FolderCog size={14} /> {update.isPending ? '수정 중' : '경로 수정'}
        </button>
      </form>
      {flatCategories.length === 0 && <p className="management-empty">수정할 폴더가 없습니다. 새 폴더를 먼저 추가해 주세요.</p>}
      {error && <div className="editor-error" role="alert">{(error as Error).message}</div>}
      <footer>폴더 이동 후 벡터 검색 경로는 문서 색인을 다시 실행하면 갱신됩니다.</footer>
    </section>
  </div>
}

function CategoryTreeItem({
  category,
  depth,
  selectedCategory,
  collapsedCategories,
  onToggle,
  onNavigate,
}: {
  category: Category
  depth: number
  selectedCategory: string | null
  collapsedCategories: string[]
  onToggle: (path: string) => void
  onNavigate: () => void
}) {
  const style = { '--category-depth': depth } as CSSProperties
  const hasChildren = category.children.length > 0
  const isCollapsed = collapsedCategories.includes(category.path)

  return (
    <div className="category-tree__branch">
      <div className="category-tree__row">
        <NavLink
          to={`/notes?category=${encodeURIComponent(category.path)}`}
          className={`nav-item category-tree__item ${depth > 0 ? 'nav-item--nested' : ''} ${selectedCategory === category.path ? 'nav-item--active' : ''}`}
          style={style}
          onClick={onNavigate}
        >
          <span className="nav-item__icon">
            {depth === 0 ? CATEGORY_ICONS[category.name] ?? <Boxes size={17} /> : <FolderClosed size={14} />}
          </span>
          <span title={category.path}>{category.name}</span>
          <small>{category.documentCount}</small>
        </NavLink>
        {hasChildren && (
          <button
            className="category-tree__toggle"
            type="button"
            aria-label={`${category.name} 하위 폴더 ${isCollapsed ? '펼치기' : '접기'}`}
            aria-expanded={!isCollapsed}
            title={isCollapsed ? '하위 폴더 펼치기' : '하위 폴더 접기'}
            onClick={() => onToggle(category.path)}
          >
            {isCollapsed ? <ChevronRight size={14} /> : <ChevronDown size={14} />}
          </button>
        )}
      </div>
      {hasChildren && (
        <div
          className={`category-tree__children ${isCollapsed ? 'category-tree__children--collapsed' : ''}`}
          aria-hidden={isCollapsed}
          inert={isCollapsed}
        >
          <div className="category-tree__children-inner">
            {category.children.map((child) => (
              <CategoryTreeItem
                category={child}
                depth={depth + 1}
                selectedCategory={selectedCategory}
                collapsedCategories={collapsedCategories}
                onToggle={onToggle}
                onNavigate={onNavigate}
                key={child.path}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
