export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; page: number; size: number }
export interface Anomaly { id: string; eventId: string; serviceName: string; severity: Severity; reason: string; recommendedAction?: string; detectedAt: string }
export interface Stats { total: number; bySeverity: Record<string, number>; byService: Record<string, number>; since: string }
export interface ServiceSummary { name: string; status: string; lastAnomalyAt?: string; anomalyCount24h: number; lastSeverity: Severity }
export type Channel = 'EMAIL' | 'SLACK' | 'WEBHOOK' | 'IN_APP'
export interface AlertRule { id: string; name: string; enabled: boolean; severityThreshold: Severity; serviceFilter?: string; channel: Channel; destination?: string; createdAt: string; updatedAt: string }
export interface AlertHistory { id: string; anomalyId: string; ruleId: string; channel: Channel; destination?: string; status: 'SENT'|'FAILED'|'DEDUPLICATED'; errorMessage?: string; sentAt: string }
export interface SecurityIncident { id: string; userId?: string; incidentType: string; severity: Severity; route?: string; ipAddress: string; description?: string; metadata?: string; createdAt: string; updatedAt: string; resolved: boolean }
export interface HealthResult { status: 'UP'|'DEGRADED'; timestamp: string; services: Record<string, { status: string; details?: unknown; message?: string }> }
export type EventType = 'HTTP_REQUEST'|'EXCEPTION'|'SYSTEM_METRIC'|'DB_QUERY'
