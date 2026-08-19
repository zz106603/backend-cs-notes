import { useQuery } from '@tanstack/react-query'
import { Archive, Boxes, ChevronRight, Database, Network, Search, ServerCog, SquareCode } from 'lucide-react'
import type { ReactNode } from 'react'
import { NavLink, useSearchParams } from 'react-router-dom'
import { api } from '../api'

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

      {isLoading && <div className="nav-loading">카테고리 정리 중...</div>}
      {categories?.map((category) => (
        <NavLink
          key={category.name}
          to={`/notes?category=${encodeURIComponent(category.name)}`}
          className={`nav-item ${selectedCategory === category.name ? 'nav-item--active' : ''}`}
          onClick={onNavigate}
        >
          <span className="nav-item__icon">{CATEGORY_ICONS[category.name] ?? <Boxes size={17} />}</span>
          <span>{category.name}</span>
          <small>{category.documentCount}</small>
        </NavLink>
      ))}

      <div className="nav-search-note">
        <Search size={15} />
        <span>제목과 경로에서 원하는 문서를 빠르게 찾아보세요.</span>
      </div>
    </nav>
  )
}
