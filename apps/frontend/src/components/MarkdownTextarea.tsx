import { useEffect, useRef } from 'react'
import { Code2, Heading2, Table2 } from 'lucide-react'
import { basicSetup, EditorView } from 'codemirror'
import { autocompletion } from '@codemirror/autocomplete'
import type { Completion, CompletionContext } from '@codemirror/autocomplete'
import { markdown } from '@codemirror/lang-markdown'
import { EditorSelection, Transaction } from '@codemirror/state'
import { keymap } from '@codemirror/view'

interface MarkdownTextareaProps {
  value: string
  visible: boolean
  onChange: (value: string) => void
}

interface MarkdownTemplate {
  label: string
  detail: string
  keywords: string[]
  text: string
  selectionStart: number
  selectionEnd?: number
}

const MARKDOWN_TEMPLATES: MarkdownTemplate[] = [
  { label: '제목 2', detail: '중간 제목을 추가합니다', keywords: ['제목2', 'h2'], text: '## ', selectionStart: 3 },
  { label: '글머리 목록', detail: '순서 없는 목록을 시작합니다', keywords: ['목록', '리스트'], text: '- ', selectionStart: 2 },
  { label: '번호 목록', detail: '순서 있는 목록을 시작합니다', keywords: ['번호목록', '번호'], text: '1. ', selectionStart: 3 },
  { label: '체크 목록', detail: '할 일 항목을 추가합니다', keywords: ['체크', '할일'], text: '- [ ] ', selectionStart: 6 },
  { label: '코드 블록', detail: '언어를 지정할 수 있는 코드 영역입니다', keywords: ['코드', 'code'], text: '```text\n\n```', selectionStart: 3, selectionEnd: 7 },
  { label: '표', detail: '2열 Markdown 표를 추가합니다', keywords: ['표', 'table'], text: '| 제목 1 | 제목 2 |\n| --- | --- |\n| 내용 1 | 내용 2 |', selectionStart: 2, selectionEnd: 6 },
  { label: '인용문', detail: '인용 영역을 시작합니다', keywords: ['인용'], text: '> ', selectionStart: 2 },
  { label: '구분선', detail: '내용 사이에 구분선을 추가합니다', keywords: ['구분선', '선'], text: '---\n', selectionStart: 4 },
]

function insertTemplate(view: EditorView, template: MarkdownTemplate, from?: number, to?: number) {
  const selection = view.state.selection.main
  const replaceFrom = from ?? selection.from
  const replaceTo = to ?? selection.to
  view.dispatch({
    changes: { from: replaceFrom, to: replaceTo, insert: template.text },
    selection: EditorSelection.range(
      replaceFrom + template.selectionStart,
      replaceFrom + (template.selectionEnd ?? template.selectionStart),
    ),
    userEvent: 'input',
  })
  view.focus()
  return true
}

function wrapSelection(view: EditorView, before: string, after: string) {
  const selection = view.state.selection.main
  const selectedText = view.state.sliceDoc(selection.from, selection.to)
  view.dispatch({
    changes: { from: selection.from, to: selection.to, insert: before + selectedText + after },
    selection: selectedText
      ? EditorSelection.range(selection.from + before.length, selection.from + before.length + selectedText.length)
      : EditorSelection.cursor(selection.from + before.length),
    userEvent: 'input',
  })
  view.focus()
  return true
}

function insertLink(view: EditorView) {
  const selection = view.state.selection.main
  const selectedText = view.state.sliceDoc(selection.from, selection.to)
  const label = selectedText || '링크 텍스트'
  const text = `[${label}](https://)`
  const urlStart = selection.from + label.length + 3
  view.dispatch({
    changes: { from: selection.from, to: selection.to, insert: text },
    selection: selectedText
      ? EditorSelection.range(urlStart, urlStart + 8)
      : EditorSelection.range(selection.from + 1, selection.from + 1 + label.length),
    userEvent: 'input',
  })
  view.focus()
  return true
}

function completionFor(template: MarkdownTemplate): Completion {
  return {
    label: template.label,
    detail: template.detail,
    type: 'keyword',
    boost: 1,
    apply(view, _completion, from, to) {
      insertTemplate(view, template, from - 1, to)
    },
  }
}

function slashCommandSource(context: CompletionContext) {
  const line = context.state.doc.lineAt(context.pos)
  const beforeCursor = context.state.sliceDoc(line.from, context.pos)
  const match = /^\/(\S*)$/.exec(beforeCursor)
  if (!match) return null
  const query = match[1].toLowerCase()
  const templates = query
    ? MARKDOWN_TEMPLATES.filter((template) =>
        template.label.replaceAll(' ', '').toLowerCase().includes(query)
        || template.keywords.some((keyword) => keyword.includes(query)),
      )
    : MARKDOWN_TEMPLATES
  return {
    from: line.from + 1,
    options: templates.map(completionFor),
    validFor: /^\S*$/,
    filter: false,
  }
}

const editorTheme = EditorView.theme({
  '&': { height: '100%' },
  '.cm-scroller': { minHeight: '610px' },
  '&.cm-focused': { outline: 'none' },
})

export function MarkdownTextarea({ value, visible, onChange }: MarkdownTextareaProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView | null>(null)
  const initialValueRef = useRef(value)
  const onChangeRef = useRef(onChange)
  onChangeRef.current = onChange

  useEffect(() => {
    if (!containerRef.current) return
    const view = new EditorView({
      doc: initialValueRef.current,
      parent: containerRef.current,
      extensions: [
        keymap.of([
          { key: 'Mod-b', run: (currentView) => wrapSelection(currentView, '**', '**') },
          { key: 'Mod-i', run: (currentView) => wrapSelection(currentView, '*', '*') },
          { key: 'Mod-k', run: insertLink },
        ]),
        EditorView.inputHandler.of((currentView, from, to, text) => {
          if (text !== '`' || from !== to || currentView.state.sliceDoc(Math.max(0, from - 2), from) !== '``') {
            return false
          }
          currentView.dispatch({
            changes: { from, to, insert: '`\n\n```' },
            selection: EditorSelection.cursor(from + 2),
            userEvent: 'input.type',
          })
          return true
        }),
        basicSetup,
        markdown(),
        autocompletion({ override: [slashCommandSource] }),
        EditorView.contentAttributes.of({ 'aria-label': 'Markdown 내용' }),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) onChangeRef.current(update.state.doc.toString())
        }),
        editorTheme,
      ],
    })
    viewRef.current = view
    return () => {
      view.destroy()
      viewRef.current = null
    }
  }, [])

  useEffect(() => {
    const view = viewRef.current
    if (!view || view.state.doc.toString() === value) return
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: value },
      annotations: Transaction.addToHistory.of(false),
    })
  }, [value])

  useEffect(() => {
    if (visible) viewRef.current?.requestMeasure()
  }, [visible])

  const applyTemplate = (template: MarkdownTemplate) => {
    const view = viewRef.current
    if (view) insertTemplate(view, template)
  }

  return (
    <div className={`markdown-editor-input ${visible ? '' : 'markdown-editor-input--hidden'}`}>
      <div className="markdown-tools" role="toolbar" aria-label="Markdown 서식">
        <button type="button" title="제목 2" onClick={() => applyTemplate(MARKDOWN_TEMPLATES[0])}><Heading2 size={14} /><span>제목</span></button>
        <button type="button" title="굵게 (Ctrl+B)" onClick={() => viewRef.current && wrapSelection(viewRef.current, '**', '**')}><strong>B</strong><span>굵게</span></button>
        <button type="button" title="코드 블록" onClick={() => applyTemplate(MARKDOWN_TEMPLATES[4])}><Code2 size={14} /><span>코드</span></button>
        <button type="button" title="표" onClick={() => applyTemplate(MARKDOWN_TEMPLATES[5])}><Table2 size={14} /><span>표</span></button>
        <small><kbd>/</kbd> 명령어 · <kbd>Ctrl+B</kbd> 굵게</small>
      </div>
      <div className="codemirror-host" ref={containerRef} />
    </div>
  )
}
