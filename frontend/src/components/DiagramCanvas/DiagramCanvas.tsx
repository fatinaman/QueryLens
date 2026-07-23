import { useEffect } from 'react'
import {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  ReactFlow,
  useEdgesState,
  useNodesState,
  type NodeTypes,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { createDiagramElements, type TableFlowNode } from '../../utils/diagram'
import type { SchemaParseResponse } from '../../types/schema'
import { TableNode } from '../TableNode/TableNode'
import './DiagramCanvas.css'

const nodeTypes: NodeTypes = {
  tableNode: TableNode,
}

interface DiagramCanvasProps {
  schema: SchemaParseResponse | null
  loading: boolean
}

export function DiagramCanvas({ schema, loading }: DiagramCanvasProps) {
  const initialElements = createDiagramElements(schema?.tables ?? [])
  const [nodes, setNodes, onNodesChange] = useNodesState<TableFlowNode>(
    initialElements.nodes,
  )
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialElements.edges)

  useEffect(() => {
    const elements = createDiagramElements(schema?.tables ?? [])
    setNodes(elements.nodes)
    setEdges(elements.edges)
  }, [schema, setEdges, setNodes])

  const isEmpty = nodes.length === 0

  return (
    <section
      className="diagram-panel"
      aria-labelledby="diagram-title"
      aria-busy={loading}
    >
      <div className="diagram-panel__heading">
        <div>
          <p className="eyebrow">02 · Interactive model</p>
          <h2 id="diagram-title">Entity relationship diagram</h2>
        </div>
        {schema && (
          <span className="diagram-panel__count">
            {schema.tables.length} {schema.tables.length === 1 ? 'table' : 'tables'}
          </span>
        )}
      </div>

      <div className="diagram-panel__canvas">
        {isEmpty ? (
          <div className="diagram-empty">
            <div className="diagram-empty__mark" aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
            <h3>Your ER diagram will appear here.</h3>
            <p>Paste a PostgreSQL schema or load the example to begin.</p>
          </div>
        ) : (
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            nodesConnectable={false}
            deleteKeyCode={null}
            minZoom={0.25}
            maxZoom={1.8}
            fitView
            fitViewOptions={{ padding: 0.2, maxZoom: 1.1 }}
            aria-label="Interactive entity relationship diagram"
          >
            <Background
              variant={BackgroundVariant.Dots}
              gap={18}
              size={1.2}
              color="#c8cede"
            />
            <Controls showInteractive={false} />
            <MiniMap
              className="diagram-minimap"
              pannable
              zoomable
              nodeColor="#d7dced"
              maskColor="rgba(239, 242, 248, 0.75)"
            />
          </ReactFlow>
        )}
      </div>
    </section>
  )
}
