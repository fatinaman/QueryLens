import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DiagramCanvas } from './DiagramCanvas'

describe('DiagramCanvas', () => {
  it('renders a helpful empty state before a schema is available', () => {
    render(<DiagramCanvas schema={null} loading={false} />)

    const panel = screen.getByRole('region', {
      name: 'Entity relationship diagram',
    })
    expect(panel).toHaveAttribute('aria-busy', 'false')
    expect(screen.getByRole('heading', {
      name: 'Your ER diagram will appear here.',
    })).toBeInTheDocument()
    expect(screen.getByText(
      'Paste a PostgreSQL schema or load the example to begin.',
    )).toBeInTheDocument()
  })
})
