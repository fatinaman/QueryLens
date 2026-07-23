import {
  MarkerType,
  type Edge,
  type Node,
} from '@xyflow/react'
import type { TableDto } from '../types/schema'

export interface TableNodeData extends Record<string, unknown> {
  table: TableDto
}

export type TableFlowNode = Node<TableNodeData, 'tableNode'>

export interface DiagramElements {
  nodes: TableFlowNode[]
  edges: Edge[]
}

const COLUMNS_PER_ROW = 3
const HORIZONTAL_GAP = 380
const VERTICAL_GAP = 300
const START_X = 40
const START_Y = 40

export function tableNodeId(tableName: string): string {
  return `table:${tableName}`
}

export function createDiagramElements(tables: TableDto[]): DiagramElements {
  const tableNames = new Set(tables.map((table) => table.name))

  const nodes: TableFlowNode[] = tables.map((table, index) => ({
    id: tableNodeId(table.name),
    type: 'tableNode',
    position: {
      x: START_X + (index % COLUMNS_PER_ROW) * HORIZONTAL_GAP,
      y: START_Y + Math.floor(index / COLUMNS_PER_ROW) * VERTICAL_GAP,
    },
    data: { table },
  }))

  const edges: Edge[] = []

  tables.forEach((table) => {
    table.foreignKeys.forEach((foreignKey, index) => {
      if (!tableNames.has(foreignKey.referencedTable)) {
        return
      }

      edges.push({
        id: [
          'fk',
          table.name,
          foreignKey.column,
          foreignKey.referencedTable,
          foreignKey.referencedColumn,
          index,
        ].join(':'),
        source: tableNodeId(table.name),
        target: tableNodeId(foreignKey.referencedTable),
        type: 'smoothstep',
        label: `${foreignKey.column} → ${foreignKey.referencedColumn}`,
        markerEnd: {
          type: MarkerType.ArrowClosed,
          width: 18,
          height: 18,
          color: '#5267a8',
        },
        style: {
          stroke: '#5267a8',
          strokeWidth: 2,
        },
        labelStyle: {
          fill: '#33416d',
          fontSize: 12,
          fontWeight: 600,
        },
        labelBgStyle: {
          fill: '#f8f9fd',
          fillOpacity: 0.94,
        },
        labelBgPadding: [6, 4],
        labelBgBorderRadius: 4,
      })
    })
  })

  return { nodes, edges }
}
