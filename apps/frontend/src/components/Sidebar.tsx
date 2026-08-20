import { useQuery } from '@tanstack/react-query'
import { Archive, Boxes, ChevronRight, Database, FolderClosed, MessageCircleQuestion, Network, RefreshCw, Search, ServerCog, SquareCode, Trash2 } from 'lucide-react'
import type { CSSProperties, ReactNode } from 'react'
import { NavLink, useSearchParams } from 'react-router-dom'
import { api } from '../api'
import type { Category } from '../types'

const CATEGORY_ICONS: Record<string, ReactNode> = {
  백엔드: <ServerCog size={17} />,
  데이터베이스: <Database size={17} />,
  네트워크: <Network size={17} />,
  프로그래밍: <SquareCode size={17} />,
  Archive: <Archive size={17} />,
}

export function Sidebar({ onNavigate }: { onNavigate: () => void }) {
  const { data: categories, isLoading } = useQuery({ queryKey: ['categories'], queryFn: api.categories })
  const [searchParams] = useSearchParams()
  const selectedCategory = searchParams.get('category')

  return (
    <nav className="sidebar-nav" aria-label="문서 카테고리">
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

      <div className="nav-search-note">
        <Search size={15} />
        <span>폴더를 선택하거나 제목과 경로에서 원하는 문서를 찾아보세요.</span>
      </div>
    </nav>
  )
}

function CategoryTreeItem({
  category,
  depth,
  selectedCategory,
  onNavigate,
}: {
  category: Category
  depth: number
  selectedCategory: string | null
  onNavigate: () => void
}) {
  const style = { '--category-depth': depth } as CSSProperties

  return (
    <div className="category-tree__branch">
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
      {category.children.length > 0 && (
        <div className="category-tree__children">
          {category.children.map((child) => (
            <CategoryTreeItem
              category={child}
              depth={depth + 1}
              selectedCategory={selectedCategory}
              onNavigate={onNavigate}
              key={child.path}
            />
          ))}
        </div>
      )}
    </div>
  )
}
