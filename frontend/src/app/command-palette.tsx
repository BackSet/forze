import { useEffect } from 'react'
import { Command } from 'cmdk'
import { Search } from 'lucide-react'

import type { Permission } from '@/lib/auth/permissions'

/**
 * A single command. When `permission` is set the action is only offered if the
 * user holds it — the palette never exposes a navigation/creation the backend
 * would reject. Actions with no permission (e.g. "Inicio") are always available.
 */
export type PaletteAction = {
  id: string
  label: string
  group: string
  permission?: Permission
  run: () => void
}

type CommandPaletteProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  permissions: string[]
  actions: PaletteAction[]
}

export function CommandPalette({ open, onOpenChange, permissions, actions }: CommandPaletteProps) {
  // Global Cmd/Ctrl+K toggle. Registered once while mounted.
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        onOpenChange(!open)
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onOpenChange])

  const allowed = actions.filter((action) => !action.permission || permissions.includes(action.permission))
  const groups = [...new Set(allowed.map((a) => a.group))]

  return (
    <Command.Dialog
      open={open}
      onOpenChange={onOpenChange}
      label="Paleta de comandos"
      className="fixed left-1/2 top-24 z-50 w-[92vw] max-w-lg -translate-x-1/2 overflow-hidden rounded-xl border border-border bg-panel text-foreground shadow-2xl animate-scale-up"
    >
      <div className="flex items-center gap-2 border-b border-border px-3">
        <Search className="size-4 text-muted-foreground" aria-hidden="true" />
        <Command.Input
          placeholder="Buscar acción o sección…"
          className="h-11 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
        />
      </div>
      <Command.List className="max-h-80 overflow-y-auto p-2">
        <Command.Empty className="py-6 text-center text-sm text-muted-foreground">
          Sin resultados.
        </Command.Empty>
        {groups.map((group) => (
          <Command.Group
            key={group}
            heading={group}
            className="px-1 pt-2 text-[10px] font-bold uppercase tracking-wider text-muted-foreground [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:pb-1"
          >
            {allowed
              .filter((a) => a.group === group)
              .map((action) => (
                <Command.Item
                  key={action.id}
                  value={`${action.group} ${action.label}`}
                  onSelect={() => {
                    action.run()
                    onOpenChange(false)
                  }}
                  className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-2 text-sm text-foreground aria-selected:bg-primary aria-selected:text-primary-foreground"
                >
                  {action.label}
                </Command.Item>
              ))}
          </Command.Group>
        ))}
      </Command.List>
    </Command.Dialog>
  )
}
