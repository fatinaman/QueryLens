import { MarkerType } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import { projectsTable, usersTable } from '../test/fixtures'
import { createDiagramElements, tableNodeId } from './diagram'

describe('diagram transformations', () => {
  it('creates deterministic IDs and three-column grid positions', () => {
    const tables = Array.from({ length: 5 }, (_, index) => ({
      ...usersTable,
      name: `table.${index}`,
    }))

    const first = createDiagramElements(tables)
    const second = createDiagramElements(tables)

    expect(first).toEqual(second)
    expect(first.nodes.map(({ id, position }) => ({ id, position }))).toEqual([
      { id: 'table:table.0', position: { x: 40, y: 40 } },
      { id: 'table:table.1', position: { x: 420, y: 40 } },
      { id: 'table:table.2', position: { x: 800, y: 40 } },
      { id: 'table:table.3', position: { x: 40, y: 340 } },
      { id: 'table:table.4', position: { x: 420, y: 340 } },
    ])
  })

  it('creates a directed, labelled foreign-key edge', () => {
    const { edges } = createDiagramElements([usersTable, projectsTable])

    expect(edges).toHaveLength(1)
    expect(edges[0]).toMatchObject({
      id: 'fk:projects:owner_id:users:id:0',
      source: 'table:projects',
      target: 'table:users',
      type: 'smoothstep',
      label: 'owner_id → id',
      markerEnd: { type: MarkerType.ArrowClosed },
    })
  })

  it('skips edges whose referenced table is absent', () => {
    const { nodes, edges } = createDiagramElements([projectsTable])

    expect(nodes).toHaveLength(1)
    expect(edges).toEqual([])
  })

  it('preserves distinct relationships with unique edge IDs', () => {
    const table = {
      ...projectsTable,
      foreignKeys: [
        projectsTable.foreignKeys[0],
        { ...projectsTable.foreignKeys[0], column: 'reviewer_id' },
      ],
    }

    const { edges } = createDiagramElements([usersTable, table])

    expect(edges).toHaveLength(2)
    expect(new Set(edges.map((edge) => edge.id)).size).toBe(2)
  })

  it('supports punctuation in deterministic table node IDs', () => {
    expect(tableNodeId('"sales"."order-items"')).toBe(
      'table:"sales"."order-items"',
    )
  })
})
