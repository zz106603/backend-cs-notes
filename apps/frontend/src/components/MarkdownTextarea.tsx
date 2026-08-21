import { useEffect, useRef, useState } from 'react'
import { CheckSquare, Code2, Heading2, List, ListOrdered, Quote, Table2, Type } from 'lucide-react'
import type { ChangeEvent, KeyboardEvent, ReactNode } from 'react'

interface MarkdownTextareaProps {
  value: string
  onChange: (value: string) => void
}

interface SlashCommand {
  name: string
  description: string
  keywords: string[]
  icon: ReactNode
  template: string
  selectionStart: number
  selectionEnd?: number
}

interface SlashMatch {
  start: number
  end: number
  query: string
}

const SLASH_COMMANDS: SlashCommand[] = [
  { name: '제목 2', description: '중간 제목을 추가합니다', keywords: ['제목2', 'h2'], icon: <Heading2 size={15} />, template: '## ', selectionStart: 3 },
  { name: '글머리 목록', description: '순서 없는 목록을 시작합니다', keywords: ['목록', '리스트'], icon: <List size={15} />, template: '- ', selectionStart: 2 },
  { name: '번호 목록', description: '순서 있는 목록을 시작합니다', keywords: ['번호목록', '번호'], icon: <ListOrdered size={15} />, template: '1. ', selectionStart: 3 },
  { name: '체크 목록', description: '할 일 항목을 추가합니다', keywords: ['체크', '할일'], icon: <CheckSquare size={15} />, template: '- [ ] ', selectionStart: 6 },
  { name: '코드 블록', description: '언어를 지정할 수 있는 코드 영역입니다', keywords: ['코드', 'code'], icon: <Code2 size={15} />, template: '```text\n\n```', selectionStart: 3, selectionEnd: 7 },
  { name: '표', description: '2열 Markdown 표를 추가합니다', keywords: ['표', 'table'], icon: <Table2 size={15} />, template: '| 제목 1 | 제목 2 |\n| --- | --- |\n| 내용 1 | 내용 2 |', selectionStart: 2, selectionEnd: 6 },
  { name: '인용문', description: '인용 영역을 시작합니다', keywords: ['인용'], icon: <Quote size={15} />, template: '> ', selectionStart: 2 },
  { name: '구분선', description: '내용 사이에 구분선을 추가합니다', keywords: ['구분선', '선'], icon: <Type size={15} />, template: '---\n', selectionStart: 4 },
]

function findSlashMatch(value: string, cursor: number): SlashMatch | null {
  const lineStart = value.lastIndexOf('\n', cursor - 1) + 1
  const line = value.slice(lineStart, cursor)
  const match = /^\/(\S*)$/.exec(line)
  return match ? { start: lineStart, end: cursor, query: match[1].toLowerCase() } : null
}

function filteredCommands(query: string) {
  if (!query) return SLASH_COMMANDS
  return SLASH_COMMANDS.filter((command) =>
    command.name.replaceAll(' ', '').toLowerCase().includes(query)
    || command.keywords.some((keyword) => keyword.includes(query)),
  )
}

export function MarkdownTextarea({ value, onChange }: MarkdownTextareaProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const commandRefs = useRef<Array<HTMLButtonElement | null>>([])
  const [slashMatch, setSlashMatch] = useState<SlashMatch | null>(null)
  const [activeCommand, setActiveCommand] = useState(0)
  const commands = filteredCommands(slashMatch?.query ?? '')

  useEffect(() => {
    if (!slashMatch || commands.length === 0) return
    commandRefs.current[activeCommand]?.scrollIntoView({ block: 'nearest' })
  }, [activeCommand, commands.length, slashMatch])

  const restoreSelection = (start: number, end = start) => {
    window.requestAnimationFrame(() => {
      textareaRef.current?.focus()
      textareaRef.current?.setSelectionRange(start, end)
    })
  }

  const replaceRange = (start: number, end: number, replacement: string, selectionStart: number, selectionEnd = selectionStart) => {
    onChange(value.slice(0, start) + replacement + value.slice(end))
    setSlashMatch(null)
    restoreSelection(start + selectionStart, start + selectionEnd)
  }

  const replaceSelection = (replacement: string, selectionStart: number, selectionEnd = selectionStart) => {
    const textarea = textareaRef.current
    if (!textarea) return
    replaceRange(textarea.selectionStart, textarea.selectionEnd, replacement, selectionStart, selectionEnd)
  }

  const wrapSelection = (before: string, after: string) => {
    const textarea = textareaRef.current
    if (!textarea) return
    const selected = value.slice(textarea.selectionStart, textarea.selectionEnd)
    const replacement = before + selected + after
    const selectionStart = before.length
    const selectionEnd = selected ? before.length + selected.length : before.length
    replaceRange(textarea.selectionStart, textarea.selectionEnd, replacement, selectionStart, selectionEnd)
  }

  const updateSlashMenu = (nextValue: string, cursor: number) => {
    const match = findSlashMatch(nextValue, cursor)
    setSlashMatch(match)
    setActiveCommand(0)
  }

  const changeContent = (event: ChangeEvent<HTMLTextAreaElement>) => {
    const nextValue = event.target.value
    onChange(nextValue)
    updateSlashMenu(nextValue, event.target.selectionStart)
  }

  const applyCommand = (command: SlashCommand) => {
    if (!slashMatch) return
    replaceRange(slashMatch.start, slashMatch.end, command.template, command.selectionStart, command.selectionEnd)
  }

  const continueList = (cursor: number) => {
    const lineStart = value.lastIndexOf('\n', cursor - 1) + 1
    const currentLine = value.slice(lineStart, cursor)
    const task = /^(\s*[-*+]\s+\[[ xX]\]\s+)(.*)$/.exec(currentLine)
    const bullet = /^(\s*[-*+]\s+)(.*)$/.exec(currentLine)
    const ordered = /^(\s*)(\d+)([.)]\s+)(.*)$/.exec(currentLine)
    const match = task ?? bullet

    if (match) {
      if (!match[2]) {
        replaceRange(lineStart, cursor, '', 0)
        return true
      }
      const prefix = task ? task[1].replace(/\[[xX]\]/, '[ ]') : match[1]
      replaceRange(cursor, cursor, `\n${prefix}`, prefix.length + 1)
      return true
    }
    if (ordered) {
      if (!ordered[4]) {
        replaceRange(lineStart, cursor, '', 0)
        return true
      }
      const prefix = `${ordered[1]}${Number(ordered[2]) + 1}${ordered[3]}`
      replaceRange(cursor, cursor, `\n${prefix}`, prefix.length + 1)
      return true
    }
    return false
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.nativeEvent.isComposing) return
    const textarea = event.currentTarget
    const start = textarea.selectionStart
    const end = textarea.selectionEnd

    if (slashMatch && commands.length > 0) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault()
        const direction = event.key === 'ArrowDown' ? 1 : -1
        setActiveCommand((current) => (current + direction + commands.length) % commands.length)
        return
      }
      if (event.key === 'Enter') {
        event.preventDefault()
        applyCommand(commands[activeCommand] ?? commands[0])
        return
      }
      if (event.key === 'Escape') {
        event.preventDefault()
        setSlashMatch(null)
        return
      }
    }

    if (event.key === '`' && start === end && value.slice(Math.max(0, start - 2), start) === '``') {
      event.preventDefault()
      replaceRange(start, end, '`\n\n```', 2)
      return
    }

    const pairs: Record<string, string> = { '(': ')', '[': ']', '{': '}', '"': '"' }
    const closing = pairs[event.key]
    if (closing === event.key && start === end && value[start] === event.key) {
      event.preventDefault()
      restoreSelection(start + 1)
      return
    }
    if (closing) {
      event.preventDefault()
      const selected = value.slice(start, end)
      replaceRange(start, end, event.key + selected + closing, 1, selected ? selected.length + 1 : 1)
      return
    }
    if ([')', ']', '}'].includes(event.key) && start === end && value[start] === event.key) {
      event.preventDefault()
      restoreSelection(start + 1)
      return
    }
    if (event.key === 'Backspace' && start === end && start > 0 && pairs[value[start - 1]] === value[start]) {
      event.preventDefault()
      replaceRange(start - 1, start + 1, '', 0)
      return
    }
    if (event.key === 'Enter' && start === end && continueList(start)) {
      event.preventDefault()
    }
  }

  return (
    <div className="markdown-editor-input">
      <div className="markdown-tools" role="toolbar" aria-label="Markdown 서식">
        <button type="button" title="제목 2" onClick={() => replaceSelection('## ', 3)}><Heading2 size={14} /><span>제목</span></button>
        <button type="button" title="굵게" onClick={() => wrapSelection('**', '**')}><strong>B</strong><span>굵게</span></button>
        <button type="button" title="코드 블록" onClick={() => replaceSelection('```text\n\n```', 3, 7)}><Code2 size={14} /><span>코드</span></button>
        <button type="button" title="표" onClick={() => replaceSelection('| 제목 1 | 제목 2 |\n| --- | --- |\n| 내용 1 | 내용 2 |', 2, 6)}><Table2 size={14} /><span>표</span></button>
        <small><kbd>/</kbd> 명령어</small>
      </div>
      <label className="editor-input-pane">
        <span className="sr-only">Markdown 내용</span>
        <textarea
          ref={textareaRef}
          value={value}
          onChange={changeContent}
          onKeyDown={handleKeyDown}
          onClick={(event) => updateSlashMenu(value, event.currentTarget.selectionStart)}
          onKeyUp={(event) => {
            if (!['ArrowDown', 'ArrowUp', 'Enter', 'Escape'].includes(event.key)) {
              updateSlashMenu(value, event.currentTarget.selectionStart)
            }
          }}
          spellCheck={false}
        />
      </label>
      {slashMatch && (
        <div className="slash-command-menu" role="listbox" aria-label="Markdown 블록 선택">
          <header><span>블록 추가</span><small>↑↓ 선택 · Enter 적용</small></header>
          {commands.length === 0 ? (
            <p>일치하는 명령어가 없습니다.</p>
          ) : commands.map((command, index) => (
            <button
              ref={(element) => { commandRefs.current[index] = element }}
              type="button"
              role="option"
              aria-selected={index === activeCommand}
              className={index === activeCommand ? 'active' : ''}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => applyCommand(command)}
              key={command.name}
            >
              <span>{command.icon}</span>
              <span><strong>{command.name}</strong><small>{command.description}</small></span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
