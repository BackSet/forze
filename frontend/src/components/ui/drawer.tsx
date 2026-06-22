import { X } from 'lucide-react'
import { useEffect, useRef, type ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

type DrawerProps = {
  open: boolean
  onClose: () => void
  title?: string
  side?: 'left' | 'right'
  children: ReactNode
  className?: string
}

/**
 * Responsive overlay drawer used for mobile navigation and side panels.
 * Closes on Escape and backdrop click, moves focus into the panel on open and
 * restores it on close, and honors `prefers-reduced-motion`.
 */
export function Drawer({ open, onClose, title, side = 'left', children, className }: DrawerProps) {
  const panelRef = useRef<HTMLDivElement>(null)
  const previouslyFocused = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    previouslyFocused.current = document.activeElement as HTMLElement | null
    panelRef.current?.focus()

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      previouslyFocused.current?.focus()
    }
  }, [open, onClose])

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50">
      <div
        className="absolute inset-0 bg-foreground/40 motion-reduce:transition-none"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={title ?? 'Panel'}
        tabIndex={-1}
        className={cn(
          'absolute inset-y-0 flex w-72 max-w-[85vw] flex-col bg-panel shadow-xl outline-none animate-fade-in motion-reduce:animate-none',
          side === 'left' ? 'left-0 border-r border-border' : 'right-0 border-l border-border',
          className,
        )}
      >
        <div className="flex h-14 items-center justify-between border-b border-border px-4">
          <span className="text-sm font-semibold">{title}</span>
          <Button variant="ghost" size="icon" onClick={onClose} aria-label="Cerrar panel">
            <X aria-hidden="true" />
          </Button>
        </div>
        <div className="flex-1 overflow-y-auto p-4">{children}</div>
      </div>
    </div>
  )
}
