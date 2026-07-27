import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { EmptyState, ErrorState } from '@/components/state-panel'

describe('data states', () => {
  it('renders an actionable error', () => {
    render(<ErrorState error={new Error('Gateway unavailable')} retry={() => undefined}/>)
    expect(screen.getByText('Gateway unavailable')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
  it('renders a useful empty state', () => {
    render(<EmptyState title="No incidents" detail="Everything is quiet."/>)
    expect(screen.getByText('No incidents')).toBeInTheDocument()
  })
})
