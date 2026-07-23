import { useEffect, useRef, useState } from 'react'
import { parseSchema, SchemaApiError } from './api/schemaApi'
import { DiagramCanvas } from './components/DiagramCanvas/DiagramCanvas'
import { SchemaEditor } from './components/SchemaEditor/SchemaEditor'
import { EXAMPLE_SCHEMA } from './constants/exampleSchema'
import type { SchemaParseResponse } from './types/schema'
import './App.css'

interface DisplayError {
  message: string
  details: string[]
}

function App() {
  const [sql, setSql] = useState('')
  const [schema, setSchema] = useState<SchemaParseResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<DisplayError | null>(null)
  const activeRequest = useRef<AbortController | null>(null)

  useEffect(() => {
    return () => activeRequest.current?.abort()
  }, [])

  async function handleGenerate() {
    if (sql.trim().length === 0 || loading) {
      return
    }

    activeRequest.current?.abort()
    const controller = new AbortController()
    activeRequest.current = controller
    setError(null)
    setLoading(true)

    try {
      const result = await parseSchema(sql, controller.signal)
      setSchema(result)
      setError(null)
    } catch (requestError) {
      if (requestError instanceof DOMException && requestError.name === 'AbortError') {
        return
      }

      if (requestError instanceof SchemaApiError) {
        setError({
          message: requestError.message,
          details: [...new Set(requestError.details)],
        })
      } else {
        setError({
          message: 'Unable to generate the diagram',
          details: ['An unexpected frontend error occurred.'],
        })
      }
    } finally {
      if (activeRequest.current === controller) {
        activeRequest.current = null
        setLoading(false)
      }
    }
  }

  function handleLoadExample() {
    setSql(EXAMPLE_SCHEMA)
    setError(null)
  }

  function handleClear() {
    activeRequest.current?.abort()
    activeRequest.current = null
    setSql('')
    setSchema(null)
    setError(null)
    setLoading(false)
  }

  const canClear = sql.length > 0 || schema !== null || error !== null

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="QueryLens home">
          <span className="brand__mark" aria-hidden="true">QL</span>
          <span>QueryLens</span>
        </a>
        <span className="site-header__status">
          <span aria-hidden="true" />
          Local developer tool
        </span>
      </header>

      <main id="top">
        <section className="hero-copy">
          <p className="eyebrow">PostgreSQL schema visualizer</p>
          <h1>
            See the structure
            <br />
            behind your SQL.
          </h1>
          <p className="hero-copy__description">
            Turn PostgreSQL <code>CREATE TABLE</code> statements into an
            interactive ER diagram.
          </p>
          <p className="limitation-notice">
            QueryLens currently supports common CREATE TABLE syntax. Advanced
            PostgreSQL features may not yet be supported.
          </p>
        </section>

        {error && (
          <section className="error-panel" role="alert" aria-live="assertive">
            <div className="error-panel__icon" aria-hidden="true">!</div>
            <div>
              <h2>{error.message}</h2>
              {error.details.length > 0 && (
                <ul>
                  {error.details.map((detail) => (
                    <li key={detail}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          </section>
        )}

        <div className="workspace">
          <SchemaEditor
            sql={sql}
            loading={loading}
            canClear={canClear}
            onSqlChange={setSql}
            onGenerate={handleGenerate}
            onLoadExample={handleLoadExample}
            onClear={handleClear}
          />
          <DiagramCanvas schema={schema} loading={loading} />
        </div>
      </main>

      <footer className="site-footer">
        <span>React · TypeScript · Spring Boot · JSqlParser</span>
        <span>No database. No persistence. Your SQL stays in the request.</span>
      </footer>
    </div>
  )
}

export default App
