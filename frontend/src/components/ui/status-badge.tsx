import { cva, type VariantProps } from 'class-variance-authority'
import { type ReactNode } from 'react'

import { cn } from '@/lib/utils'

const statusBadgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium',
  {
    variants: {
      tone: {
        neutral: 'border-border bg-muted text-muted-foreground',
        info: 'border-primary/30 bg-primary/10 text-primary',
        success: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
        warning: 'border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400',
        danger: 'border-destructive/30 bg-destructive/10 text-destructive',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
)

type StatusBadgeProps = VariantProps<typeof statusBadgeVariants> & {
  children: ReactNode
  /** Optional leading icon; the label text always conveys meaning (not color alone). */
  icon?: ReactNode
  className?: string
}

/** Small status pill. Meaning is carried by the label text, not only the color. */
export function StatusBadge({ tone, icon, children, className }: StatusBadgeProps) {
  return (
    <span className={cn(statusBadgeVariants({ tone }), className)}>
      {icon}
      {children}
    </span>
  )
}
