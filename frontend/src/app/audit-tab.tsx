import { useQuery } from '@tanstack/react-query'
import { Calendar, Layers, ShieldCheck, Activity } from 'lucide-react'

import { api } from '@/lib/api/client'

export function AuditTab() {
  // Query
  const auditLogsQuery = useQuery(api.queryOptions('get', '/api/audit-logs'))

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Registro de Auditoría</h1>
          <p className="text-sm text-muted-foreground">Monitorea los eventos de negocio, cambios de precios y aprobaciones en la organización.</p>
        </div>
      </div>

      <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
        <h2 className="text-base font-bold flex items-center gap-2">
          <Activity className="size-5 text-primary" />
          Bitácora de Eventos de Negocio
        </h2>

        {auditLogsQuery.isLoading ? (
          <div className="space-y-3">
            <div className="h-10 w-full animate-pulse bg-muted rounded-lg" />
            <div className="h-10 w-full animate-pulse bg-muted rounded-lg" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                  <th className="py-2.5 px-3">Fecha y Hora</th>
                  <th className="py-2.5 px-3">Acción</th>
                  <th className="py-2.5 px-3">Entidad</th>
                  <th className="py-2.5 px-3">ID de Entidad</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {auditLogsQuery.data?.map(log => (
                  <tr key={log.id} className="hover:bg-accent/30 transition-colors">
                    <td className="py-3 px-3 text-xs text-muted-foreground font-mono flex items-center gap-1.5">
                      <Calendar className="size-3.5" />
                      {log.occurredAt ? new Date(log.occurredAt).toLocaleString() : '-'}
                    </td>
                    <td className="py-3 px-3">
                      <span className="inline-flex items-center gap-1 rounded px-2 py-0.5 text-xs font-semibold bg-primary/10 text-primary">
                        <ShieldCheck className="size-3" />
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 px-3 font-medium text-xs flex items-center gap-1 mt-1">
                      <Layers className="size-3.5 text-muted-foreground" />
                      {log.entityType}
                    </td>
                    <td className="py-3 px-3 text-xs text-muted-foreground font-mono">
                      {log.entityId || '-'}
                    </td>
                  </tr>
                ))}
                {(!auditLogsQuery.data || auditLogsQuery.data.length === 0) && (
                  <tr>
                    <td colSpan={4} className="py-8 text-center text-muted-foreground border border-dashed border-border rounded-xl">
                      No hay registros de auditoría en esta organización todavía.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
