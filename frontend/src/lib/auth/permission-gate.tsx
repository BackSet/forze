import { type ReactNode } from 'react'

import { usePermission, type Permission } from '@/lib/auth/permissions'

/** Renders children only when the current user holds the given permission. */
export function PermissionGate({
  permission,
  children,
  fallback = null,
}: {
  permission: Permission
  children: ReactNode
  fallback?: ReactNode
}) {
  const allowed = usePermission(permission)
  return <>{allowed ? children : fallback}</>
}
