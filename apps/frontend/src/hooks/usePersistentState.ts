import { useState } from 'react'

export function usePersistentState<T>(key: string, fallback: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const stored = window.localStorage.getItem(key)
      return stored === null ? fallback : JSON.parse(stored) as T
    } catch {
      return fallback
    }
  })

  const persist = (next: T) => {
    setValue(next)
    window.localStorage.setItem(key, JSON.stringify(next))
  }

  return [value, persist] as const
}
