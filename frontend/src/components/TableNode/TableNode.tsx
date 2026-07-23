import {
  Handle,
  Position,
  type NodeProps,
} from '@xyflow/react'
import type { TableFlowNode } from '../../utils/diagram'
import './TableNode.css'

export function TableNode({ data }: NodeProps<TableFlowNode>) {
  const { table } = data
  const foreignKeyColumns = new Set(
    table.foreignKeys.map((foreignKey) => foreignKey.column),
  )

  return (
    <article className="table-node" aria-label={`Table ${table.name}`}>
      <Handle
        id="references-target"
        type="target"
        position={Position.Left}
        className="table-node__handle"
      />
      <header className="table-node__header">
        <span className="table-node__kind">TABLE</span>
        <strong title={table.name}>{table.name}</strong>
      </header>
      <div className="table-node__columns">
        {table.columns.map((column) => {
          const isForeignKey = foreignKeyColumns.has(column.name)

          return (
            <div className="table-node__column" key={column.name}>
              <span className="table-node__badges" aria-label="Column keys">
                {column.primaryKey && (
                  <span className="key-badge key-badge--primary" title="Primary key">
                    PK
                  </span>
                )}
                {isForeignKey && (
                  <span className="key-badge key-badge--foreign" title="Foreign key">
                    FK
                  </span>
                )}
              </span>
              <span className="table-node__column-name">{column.name}</span>
              <span className="table-node__column-type">{column.dataType}</span>
              <span
                className="table-node__nullable"
                title={column.nullable ? 'Nullable' : 'Required'}
              >
                {column.nullable ? 'NULL' : 'REQ'}
              </span>
            </div>
          )
        })}
      </div>
      <Handle
        id="references-source"
        type="source"
        position={Position.Right}
        className="table-node__handle"
      />
    </article>
  )
}
