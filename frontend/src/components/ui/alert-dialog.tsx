import { AlertDialog as Primitive } from 'radix-ui'
import type { PropsWithChildren } from 'react'

export function AlertDialog({ open, onOpenChange, title, description, children }: PropsWithChildren<{ open: boolean; onOpenChange: (open: boolean) => void; title: string; description: string }>) {
  return <Primitive.Root open={open} onOpenChange={onOpenChange}><Primitive.Portal><Primitive.Overlay className="fixed inset-0 z-40 bg-black/70"/><Primitive.Content className="fixed left-1/2 top-1/2 z-50 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-lg border bg-card p-6 shadow-xl"><Primitive.Title className="text-lg font-semibold">{title}</Primitive.Title><Primitive.Description className="mt-1 text-sm text-muted-foreground">{description}</Primitive.Description><div className="mt-5">{children}</div></Primitive.Content></Primitive.Portal></Primitive.Root>
}
