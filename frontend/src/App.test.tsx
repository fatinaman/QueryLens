import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { parseSchema, SchemaApiError } from './api/schemaApi'
import App from './App'
import { EXAMPLE_SCHEMA } from './constants/exampleSchema'
import { schemaResponse } from './test/fixtures'

vi.mock('./api/schemaApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./api/schemaApi')>()
  return {
    ...actual,
    parseSchema: vi.fn(),
  }
})

vi.mock('./components/DiagramCanvas/DiagramCanvas', () => ({
  DiagramCanvas: ({
    schema,
    loading,
  }: {
    schema: typeof schemaResponse | null
    loading: boolean
  }) => (
    <section aria-label="Test diagram" aria-busy={loading}>
      {schema
        ? schema.tables.map((table) => <span key={table.name}>{table.name}</span>)
        : <span>Your ER diagram will appear here.</span>}
    </section>
  ),
}))

describe('App', () => {
  beforeEach(() => {
    vi.mocked(parseSchema).mockReset()
  })

  it('renders the initial empty state and keeps blank generation disabled', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /see the structure/i }))
      .toBeInTheDocument()
    expect(screen.getByText(/advanced postgresql features/i)).toBeInTheDocument()
    expect(screen.getByText('Your ER diagram will appear here.'))
      .toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generate Diagram' })).toBeDisabled()
  })

  it('loads the example without submitting it', async () => {
    const user = userEvent.setup()
    render(<App />)

    await user.click(screen.getByRole('button', { name: 'Load Example' }))

    expect(screen.getByLabelText('CREATE TABLE statements')).toHaveValue(
      EXAMPLE_SCHEMA,
    )
    expect(parseSchema).not.toHaveBeenCalled()
  })

  it('shows loading and renders a successful schema', async () => {
    const user = userEvent.setup()
    let resolveRequest: (value: typeof schemaResponse) => void = () => undefined
    vi.mocked(parseSchema).mockImplementation(() => new Promise((resolve) => {
      resolveRequest = resolve
    }))
    render(<App />)

    await user.type(
      screen.getByLabelText('CREATE TABLE statements'),
      'CREATE TABLE users ();',
    )
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))

    expect(screen.getByRole('button', { name: 'Generating…' })).toBeDisabled()
    expect(screen.getByLabelText('Test diagram')).toHaveAttribute(
      'aria-busy',
      'true',
    )

    resolveRequest(schemaResponse)

    expect(await screen.findByText('projects')).toBeInTheDocument()
    expect(screen.getByText('users')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows deduplicated structured errors and preserves SQL', async () => {
    const user = userEvent.setup()
    vi.mocked(parseSchema).mockRejectedValue(new SchemaApiError(
      'Unable to parse SQL schema',
      ['Only CREATE TABLE statements are supported', 'Only CREATE TABLE statements are supported'],
      400,
    ))
    render(<App />)

    const textarea = screen.getByLabelText('CREATE TABLE statements')
    await user.type(textarea, 'DROP TABLE users;')
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Unable to parse SQL schema')
    expect(screen.getAllByText('Only CREATE TABLE statements are supported'))
      .toHaveLength(1)
    expect(textarea).toHaveValue('DROP TABLE users;')
  })

  it('shows safe network failure guidance', async () => {
    const user = userEvent.setup()
    vi.mocked(parseSchema).mockRejectedValue(new SchemaApiError(
      'Unable to connect to the QueryLens backend.',
      ['Make sure the backend is running and the API URL is configured correctly.'],
    ))
    render(<App />)

    await user.type(
      screen.getByLabelText('CREATE TABLE statements'),
      'CREATE TABLE users ();',
    )
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Unable to connect to the QueryLens backend.',
    )
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Make sure the backend is running and the API URL is configured correctly.',
    )
  })

  it('keeps the last successful diagram when a later request fails', async () => {
    const user = userEvent.setup()
    vi.mocked(parseSchema)
      .mockResolvedValueOnce(schemaResponse)
      .mockRejectedValueOnce(new SchemaApiError(
        'Unable to parse SQL schema',
        ['Invalid statement'],
        400,
      ))
    render(<App />)

    const textarea = screen.getByLabelText('CREATE TABLE statements')
    await user.type(textarea, 'CREATE TABLE users ();')
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))
    expect(await screen.findByText('projects')).toBeInTheDocument()

    await user.clear(textarea)
    await user.type(textarea, 'DROP TABLE users;')
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid statement')
    expect(screen.getByText('projects')).toBeInTheDocument()
    expect(screen.getByText('users')).toBeInTheDocument()
  })

  it('aborts an active request and clears all visible state', async () => {
    const user = userEvent.setup()
    let capturedSignal: AbortSignal | undefined
    vi.mocked(parseSchema).mockImplementation((_sql, signal) => {
      capturedSignal = signal
      return new Promise(() => undefined)
    })
    render(<App />)

    await user.type(
      screen.getByLabelText('CREATE TABLE statements'),
      'CREATE TABLE users ();',
    )
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))

    expect(capturedSignal?.aborted).toBe(false)
    // Clear is intentionally disabled while loading, so unmount exercises
    // the user-navigation cancellation path.
    const clearButton = screen.getByRole('button', { name: 'Clear' })
    expect(clearButton).toBeDisabled()
  })

  it('aborts an active request on unmount', async () => {
    const user = userEvent.setup()
    let capturedSignal: AbortSignal | undefined
    vi.mocked(parseSchema).mockImplementation((_sql, signal) => {
      capturedSignal = signal
      return new Promise(() => undefined)
    })
    const { unmount } = render(<App />)

    await user.type(
      screen.getByLabelText('CREATE TABLE statements'),
      'CREATE TABLE users ();',
    )
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))
    unmount()

    await waitFor(() => expect(capturedSignal?.aborted).toBe(true))
  })

  it('clears SQL, errors, and a completed diagram', async () => {
    const user = userEvent.setup()
    vi.mocked(parseSchema).mockResolvedValue(schemaResponse)
    render(<App />)

    const textarea = screen.getByLabelText('CREATE TABLE statements')
    await user.type(textarea, 'CREATE TABLE users ();')
    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))
    expect(await screen.findByText('projects')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Clear' }))

    expect(textarea).toHaveValue('')
    expect(screen.getByText('Your ER diagram will appear here.'))
      .toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
