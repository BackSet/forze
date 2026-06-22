import { Loader2, Wand2 } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

type CodeFieldProps = {
  id?: string
  label?: string
  value: string
  onChange: (value: string) => void
  /**
   * Optional generator. When provided a "Generar" button appears and fills the
   * field with the resolved code. Backend remains the source of uniqueness; the
   * field stays editable so a manual code is still allowed.
   */
  onGenerate?: () => Promise<string>
  placeholder?: string
  error?: string
  disabled?: boolean
  className?: string
}

/**
 * Reusable code input with an optional backend-backed "generate next code"
 * action. Designed to wire to the MVP 1 endpoints via `onGenerate`, but works
 * standalone (manual entry) when no generator is passed.
 */
export function CodeField({
  id = 'code',
  label = 'Código',
  value,
  onChange,
  onGenerate,
  placeholder = 'Ej. PRY-2026-0001',
  error,
  disabled = false,
  className,
}: CodeFieldProps) {
  const [generating, setGenerating] = useState(false)
  const errorId = error ? `${id}-error` : undefined

  async function handleGenerate() {
    if (!onGenerate) {
      return
    }
    setGenerating(true)
    try {
      const next = await onGenerate()
      onChange(next)
    }
    finally {
      setGenerating(false)
    }
  }

  return (
    <div className={cn('space-y-1', className)}>
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <div className="flex gap-2">
        <Input
          id={id}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          aria-describedby={errorId}
        />
        {onGenerate ? (
          <Button
            type="button"
            variant="secondary"
            onClick={handleGenerate}
            disabled={disabled || generating}
            aria-label="Generar código sugerido"
          >
            {generating ? (
              <Loader2 className="animate-spin motion-reduce:animate-none" aria-hidden="true" />
            ) : (
              <Wand2 aria-hidden="true" />
            )}
            Generar
          </Button>
        ) : null}
      </div>
      {error ? (
        <p id={errorId} className="text-xs text-destructive" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  )
}
