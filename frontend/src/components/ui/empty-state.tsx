import { type LucideIcon } from 'lucide-react'
import { type ReactNode } from 'react'

import { cn } from '@/lib/utils'

type EmptyStateProps = {
  icon?: LucideIcon
  title: string
  message?: string
  action?: ReactNode
  className?: string
}

/** Actionable empty/zero state for lists and panels. */
export function EmptyState({ icon: Icon, title, message, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border px-6 py-12 text-center',
        className,
      )}
      role="status"
    >
      {Icon ? <Icon className="size-8 text-muted-foreground" aria-hidden="true" /> : null}
      <div className="space-y-1">
        <p className="text-sm font-semibold">{title}</p>
        {message ? <p className="mx-auto max-w-sm text-sm text-muted-foreground">{message}</p> : null}
      </div>
      {action ? <div className="mt-1">{action}</div> : null}
    </div>
  )
}
