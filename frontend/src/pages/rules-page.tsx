import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Power, Trash2 } from 'lucide-react'
import { api } from '@/lib/api'
import type { AlertRule } from '@/lib/types'
import { useToken } from '@/hooks/use-token'
import { PageHeader } from '@/components/page-header'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog } from '@/components/ui/dialog'
import { AlertDialog } from '@/components/ui/alert-dialog'
import { EmptyState, ErrorState, LoadingState } from '@/components/state-panel'
import { RuleForm, type RuleValues } from '@/components/rule-form'
import { formatDate } from '@/lib/utils'

export function RulesPage() {
  const token = useToken()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<AlertRule | null | undefined>()
  const [deleting, setDeleting] = useState<AlertRule>()
  const query = useQuery({ queryKey: ['rules'], queryFn: () => api<AlertRule[]>('/api/alert-rules', token), refetchInterval: 5000 })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['rules'] })
  const save = useMutation({ mutationFn: ({ values, id }: { values: RuleValues; id?: string }) => api<AlertRule>(id ? `/api/alert-rules/${id}` : '/api/alert-rules', token, { method: id ? 'PUT' : 'POST', body: JSON.stringify(values) }), onSuccess: () => { void refresh(); setEditing(undefined) } })
  const toggle = useMutation({ mutationFn: (id: string) => api<AlertRule>(`/api/alert-rules/${id}/toggle`, token, { method: 'PATCH' }), onSuccess: () => void refresh() })
  const remove = useMutation({ mutationFn: (id: string) => api(`/api/alert-rules/${id}`, token, { method: 'DELETE' }), onSuccess: () => { void refresh(); setDeleting(undefined) } })

  return <>
    <PageHeader title="Alert rules" description="Route anomalies to in-app or external notification channels." actions={<Button onClick={() => setEditing(null)}><Plus size={16}/>New rule</Button>}/>
    {query.isLoading ? <LoadingState/> : query.error ? <ErrorState error={query.error} retry={() => void query.refetch()}/> : !query.data?.length ? <EmptyState title="No alert rules" detail="Create an in-app rule to start recording alert history."/> :
      <Card className="overflow-hidden"><div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-muted/50 text-xs uppercase text-muted-foreground"><tr><th className="p-4">Rule</th><th>Threshold</th><th>Channel</th><th>Status</th><th className="pr-4 text-right">Actions</th></tr></thead><tbody className="divide-y">{query.data.map(rule => <tr key={rule.id}><td className="p-4"><div className="font-medium">{rule.name}</div><div className="text-xs text-muted-foreground">{rule.serviceFilter || 'All services'} · {formatDate(rule.updatedAt)}</div></td><td><Badge tone={rule.severityThreshold}>{rule.severityThreshold}</Badge></td><td>{rule.channel}</td><td><Badge tone={rule.enabled ? 'UP' : 'neutral'}>{rule.enabled ? 'Enabled' : 'Disabled'}</Badge></td><td><div className="flex justify-end gap-1 pr-3"><Button size="icon" variant="ghost" aria-label="Toggle rule" onClick={() => toggle.mutate(rule.id)}><Power size={16}/></Button><Button size="icon" variant="ghost" aria-label="Edit rule" onClick={() => setEditing(rule)}><Pencil size={16}/></Button><Button size="icon" variant="ghost" aria-label="Delete rule" onClick={() => setDeleting(rule)}><Trash2 size={16}/></Button></div></td></tr>)}</tbody></table></div></Card>}
    <Dialog open={editing !== undefined} onOpenChange={open => !open && setEditing(undefined)} title={editing ? 'Edit alert rule' : 'Create alert rule'} description="Rules are evaluated whenever the AI agent publishes an anomaly."><RuleForm key={editing?.id || 'new'} rule={editing || undefined} busy={save.isPending} onSubmit={values => save.mutate({ values, id: editing?.id })}/>{save.error && <p className="mt-3 text-sm text-red-400">{save.error.message}</p>}</Dialog>
    <AlertDialog open={!!deleting} onOpenChange={open => !open && setDeleting(undefined)} title="Delete alert rule" description="This action cannot be undone."><p className="text-sm text-muted-foreground">Delete “{deleting?.name}” permanently?</p><div className="mt-5 flex justify-end gap-2"><Button variant="outline" onClick={() => setDeleting(undefined)}>Cancel</Button><Button variant="destructive" disabled={remove.isPending} onClick={() => deleting && remove.mutate(deleting.id)}>Delete rule</Button></div></AlertDialog>
  </>
}
