import { useQuery } from '@tanstack/react-query'
import { Archive, Boxes, ChevronDown, ChevronRight, Database, FilePlus2, FlaskConical, FolderClosed, MessageCircleQuestion, Network, RefreshCw, Search, ServerCog, SquareCode, Trash2 } from 'lucide-react'
import type { CSSProperties, ReactNode } from 'react'
import { NavLink, useSearchParams } from 'react-router-dom'
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
  const [searchParams] = useSearchParams()
  const selectedCategory = searchParams.get('category')
  const [collapsedCategories, setCollapsedCategories] = usePersistentState<string[]>('cs-notes-collapsed-categories', [])

  const toggleCategory = (path: string) => {
    setCollapsedCategories(collapsedCategories.includes(path)
      ? collapsedCategories.filter((collapsedPath) => collapsedPath !== path)
      : [...collapsedCategories, path])
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
    </nav>
  )
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
