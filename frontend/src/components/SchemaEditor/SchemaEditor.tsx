import './SchemaEditor.css'

const SQL_LIMIT = 100_000

interface SchemaEditorProps {
  sql: string
  loading: boolean
  canClear: boolean
  onSqlChange: (value: string) => void
  onGenerate: () => void
  onLoadExample: () => void
  onClear: () => void
}

export function SchemaEditor({
  sql,
  loading,
  canClear,
  onSqlChange,
  onGenerate,
  onLoadExample,
  onClear,
}: SchemaEditorProps) {
  const isBlank = sql.trim().length === 0

  return (
    <section className="schema-editor" aria-labelledby="schema-editor-title">
      <div className="schema-editor__heading">
        <div>
          <p className="eyebrow">01 · Schema input</p>
          <h2 id="schema-editor-title">PostgreSQL DDL</h2>
        </div>
        <span className="schema-editor__shortcut">Ctrl + Enter to generate</span>
      </div>

      <label className="schema-editor__label" htmlFor="schema-sql">
        CREATE TABLE statements
      </label>
      <textarea
        id="schema-sql"
        className="schema-editor__textarea"
        value={sql}
        maxLength={SQL_LIMIT}
        spellCheck={false}
        aria-describedby="schema-sql-count schema-sql-help"
        aria-busy={loading}
        placeholder={'CREATE TABLE users (\n    id BIGSERIAL PRIMARY KEY\n);'}
        onChange={(event) => onSqlChange(event.target.value)}
        onKeyDown={(event) => {
          if (
            event.ctrlKey &&
            event.key === 'Enter' &&
            !loading &&
            !isBlank
          ) {
            event.preventDefault()
            onGenerate()
          }
        }}
      />

      <div className="schema-editor__meta">
        <span id="schema-sql-help">Multiple CREATE TABLE statements supported.</span>
        <span id="schema-sql-count">
          {sql.length.toLocaleString()} / {SQL_LIMIT.toLocaleString()} characters
        </span>
      </div>

      <div className="schema-editor__actions">
        <button
          className="button button--primary"
          type="button"
          disabled={loading || isBlank}
          onClick={onGenerate}
        >
          {loading ? 'Generating…' : 'Generate Diagram'}
        </button>
        <button
          className="button button--secondary"
          type="button"
          disabled={loading}
          onClick={onLoadExample}
        >
          Load Example
        </button>
        <button
          className="button button--quiet"
          type="button"
          disabled={loading || !canClear}
          onClick={onClear}
        >
          Clear
        </button>
      </div>
    </section>
  )
}
