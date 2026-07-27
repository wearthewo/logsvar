import type { HTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '@/lib/utils'
export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <section className={cn('rounded-lg border bg-card text-card-foreground shadow-sm', className)} {...props}/> }
export function CardHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn('flex flex-col gap-1.5 p-5', className)} {...props}/> }
export function CardTitle({ children }: PropsWithChildren) { return <h2 className="font-semibold tracking-tight">{children}</h2> }
export function CardDescription({ children }: PropsWithChildren) { return <p className="text-sm text-muted-foreground">{children}</p> }
export function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) { return <div className={cn('p-5 pt-0', className)} {...props}/> }
