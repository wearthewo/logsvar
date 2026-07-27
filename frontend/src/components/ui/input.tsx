import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'
export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) { return <input className={cn('h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring', className)} {...props}/> }
export function Select({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) { return <select className={cn('h-10 rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring', className)} {...props}/> }
export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) { return <textarea className={cn('min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring', className)} {...props}/> }
export function Label({ children, htmlFor }: { children: React.ReactNode; htmlFor?: string }) { return <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium">{children}</label> }
