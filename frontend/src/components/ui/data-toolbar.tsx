import { Search } from 'lucide-react'
import { type ReactNode } from 'react'

import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

type DataToolbarProps = {
  search?: string
  onSearchChange?: (value: string) => void
  searchPlaceholder?: string
  searchLabel?: string
  actions?: ReactNode
  className?: string
}

/**
 * Toolbar above lists/tables: an optional search box and an actions slot.
 * Stacks on mobile, row on larger screens.
 */
export function DataToolbar({
  search,
  onSearchChange,
  searchPlaceholder = 'Buscar…',
  searchLabel = 'Buscar',
  actions,
  className,
}: DataToolbarProps) {
  return (
    <div className={cn('flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between', className)}>
      {onSearchChange ? (
        <div className="relative w-full sm:max-w-xs">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
          <Input
            type="search"
            aria-label={searchLabel}
            placeholder={searchPlaceholder}
            value={search ?? ''}
            onChange={(event) => onSearchChange(event.target.value)}
            className="pl-8"
          />
        </div>
      ) : (
        <span />
      )}
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  )
}
