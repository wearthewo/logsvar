import { Slot } from 'radix-ui'
import { cva, type VariantProps } from 'class-variance-authority'
import type { ButtonHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

const variants = cva('inline-flex items-center justify-center gap-2 rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50', { variants: { variant: { default: 'bg-primary text-primary-foreground hover:opacity-90', secondary: 'bg-secondary text-secondary-foreground hover:bg-muted', outline: 'border bg-transparent hover:bg-muted', ghost: 'hover:bg-muted', destructive: 'bg-destructive text-white hover:opacity-90' }, size: { default: 'h-10 px-4', sm: 'h-8 px-3', icon: 'h-9 w-9' } }, defaultVariants: { variant: 'default', size: 'default' } })
interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof variants> { asChild?: boolean }
export function Button({ className, variant, size, asChild, ...props }: ButtonProps) { const Comp = asChild ? Slot.Root : 'button'; return <Comp className={cn(variants({ variant, size }), className)} {...props}/> }
