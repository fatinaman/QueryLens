import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { projectsTable } from '../../test/fixtures'
import { TableNode } from './TableNode'

vi.mock('@xyflow/react', () => ({
  Handle: ({ id, type }: { id: string; type: string }) => (
    <span data-testid={`${type}-handle`} data-handle-id={id} />
  ),
  Position: {
    Left: 'left',
    Right: 'right',
  },
}))

describe('TableNode', () => {
  it('renders table and ordered column details with textual key indicators', () => {
    const props = {
      id: 'table:projects',
      data: { table: projectsTable },
      type: 'tableNode',
      selected: false,
      dragging: false,
      deletable: false,
      selectable: true,
      connectable: false,
      zIndex: 0,
      isConnectable: false,
      positionAbsoluteX: 0,
      positionAbsoluteY: 0,
    } as unknown as React.ComponentProps<typeof TableNode>

    render(<TableNode {...props} />)

    const table = screen.getByRole('article', { name: 'Table projects' })
    const columns = table.querySelectorAll('.table-node__column')

    expect(within(table).getByText('projects')).toBeInTheDocument()
    expect(columns).toHaveLength(2)
    expect(columns[0]).toHaveTextContent('PK')
    expect(columns[0]).toHaveTextContent('id')
    expect(columns[0]).toHaveTextContent('BIGSERIAL')
    expect(columns[0]).toHaveTextContent('REQ')
    expect(columns[1]).toHaveTextContent('FK')
    expect(columns[1]).toHaveTextContent('owner_id')
    expect(columns[1]).toHaveTextContent('BIGINT')
    expect(columns[1]).toHaveTextContent('NULL')
  })

  it('renders matching source and target handles', () => {
    const props = {
      id: 'table:projects',
      data: { table: projectsTable },
    } as unknown as React.ComponentProps<typeof TableNode>

    render(<TableNode {...props} />)

    expect(screen.getByTestId('target-handle')).toHaveAttribute(
      'data-handle-id',
      'references-target',
    )
    expect(screen.getByTestId('source-handle')).toHaveAttribute(
      'data-handle-id',
      'references-source',
    )
  })
})
