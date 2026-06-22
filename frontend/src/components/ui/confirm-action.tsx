import { useEffect, useState, type ReactNode } from 'react'

import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

type ConfirmActionProps = {
  /** The trigger content (e.g. an icon or label). */
  children: ReactNode
  onConfirm: () => void
  /** Confirmation question shown before firing. */
  message?: string
  confirmLabel?: string
  cancelLabel?: string
  triggerLabel: string
  destructive?: boolean
  disabled?: boolean
  className?: string
}

/**
 * A trigger that requires explicit confirmation before running `onConfirm`,
 * without pulling in a dialog library. The confirmation popover closes on
 * Escape and conveys intent with text (not color alone).
 */
export function ConfirmAction({
  children,
  onConfirm,
  message = '¿Confirmar esta acción?',
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  triggerLabel,
  destructive = false,
  disabled = false,
  className,
}: ConfirmActionProps) {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!open) {
      return
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open])

  return (
    <span className={cn('relative inline-flex', className)}>
      <Button
        type="button"
        variant="secondary"
        size="icon"
        className={destructive ? 'text-destructive hover:text-destructive' : undefined}
        disabled={disabled}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={triggerLabel}
        onClick={() => setOpen((value) => !value)}
      >
        {children}
      </Button>
      {open ? (
        <div
          role="alertdialog"
          aria-label={triggerLabel}
          className="absolute right-0 top-full z-20 mt-1 w-56 rounded-lg border border-border bg-panel p-3 shadow-lg animate-scale-up motion-reduce:animate-none"
        >
          <p className="text-xs text-foreground">{message}</p>
          <div className="mt-3 flex justify-end gap-2">
            <Button type="button" variant="ghost" size="sm" onClick={() => setOpen(false)}>
              {cancelLabel}
            </Button>
            <Button
              autoFocus
              type="button"
              variant="default"
              size="sm"
              className={destructive ? 'bg-destructive text-white hover:bg-destructive/90' : undefined}
              onClick={() => {
                setOpen(false)
                onConfirm()
              }}
            >
              {confirmLabel}
            </Button>
          </div>
        </div>
      ) : null}
    </span>
  )
}
