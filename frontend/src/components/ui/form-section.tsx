import { type ReactNode } from 'react'

import { cn } from '@/lib/utils'

type FormSectionProps = {
  title: string
  description?: string
  children: ReactNode
  className?: string
}

/** Groups related form fields under a titled section (fieldset semantics). */
export function FormSection({ title, description, children, className }: FormSectionProps) {
  return (
    <fieldset className={cn('rounded-xl border border-border bg-panel p-5', className)}>
      <legend className="px-1 text-sm font-semibold">{title}</legend>
      {description ? <p className="mt-1 text-xs text-muted-foreground">{description}</p> : null}
      <div className="mt-4 space-y-4">{children}</div>
    </fieldset>
  )
}
