import { type ReactNode } from 'react'

import { cn } from '@/lib/utils'

type QuickActionsBarProps = {
  children: ReactNode
  className?: string
  'aria-label'?: string
}

/**
 * Horizontal bar of primary actions. Wraps on small screens so buttons stay
 * reachable (compact) instead of overflowing.
 */
export function QuickActionsBar({ children, className, 'aria-label': ariaLabel = 'Acciones rápidas' }: QuickActionsBarProps) {
  return (
    <div role="toolbar" aria-label={ariaLabel} className={cn('flex flex-wrap items-center gap-2', className)}>
      {children}
    </div>
  )
}
