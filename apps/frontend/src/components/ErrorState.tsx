import { CircleAlert } from 'lucide-react'

export function ErrorState({ message }: { message: string }) {
  return (
    <div className="state-panel state-panel--error" role="alert">
      <CircleAlert size={24} />
      <strong>잠시 멈췄어요</strong>
      <p>{message}</p>
    </div>
  )
}
