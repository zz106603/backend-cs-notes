export function LoadingState({ label = '문서를 불러오는 중' }: { label?: string }) {
  return (
    <div className="state-panel" role="status">
      <span className="loader" />
      <p>{label}</p>
    </div>
  )
}
