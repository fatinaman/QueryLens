import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SchemaEditor } from './SchemaEditor'

function renderEditor(overrides: Partial<React.ComponentProps<typeof SchemaEditor>> = {}) {
  const props: React.ComponentProps<typeof SchemaEditor> = {
    sql: '',
    loading: false,
    canClear: false,
    onSqlChange: vi.fn(),
    onGenerate: vi.fn(),
    onLoadExample: vi.fn(),
    onClear: vi.fn(),
    ...overrides,
  }

  render(<SchemaEditor {...props} />)
  return props
}

describe('SchemaEditor', () => {
  it('renders an accessible textarea, limit, and initial disabled actions', () => {
    renderEditor()

    const textarea = screen.getByLabelText('CREATE TABLE statements')
    expect(textarea).toHaveAttribute('maxlength', '100000')
    expect(textarea).toHaveAttribute(
      'aria-describedby',
      'schema-sql-count schema-sql-help',
    )
    expect(screen.getByText('0 / 100,000 characters')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generate Diagram' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Clear' })).toBeDisabled()
  })

  it('reports text changes through its callback', () => {
    const props = renderEditor()

    fireEvent.change(
      screen.getByLabelText('CREATE TABLE statements'),
      { target: { value: 'CREATE TABLE users ();' } },
    )

    expect(props.onSqlChange).toHaveBeenCalledWith('CREATE TABLE users ();')
  })

  it('invokes each enabled action', async () => {
    const user = userEvent.setup()
    const props = renderEditor({ sql: 'CREATE TABLE users ();', canClear: true })

    await user.click(screen.getByRole('button', { name: 'Generate Diagram' }))
    await user.click(screen.getByRole('button', { name: 'Load Example' }))
    await user.click(screen.getByRole('button', { name: 'Clear' }))

    expect(props.onGenerate).toHaveBeenCalledOnce()
    expect(props.onLoadExample).toHaveBeenCalledOnce()
    expect(props.onClear).toHaveBeenCalledOnce()
  })

  it('generates with Ctrl+Enter for non-blank SQL', async () => {
    const user = userEvent.setup()
    const props = renderEditor({ sql: 'CREATE TABLE users ();' })

    await user.click(screen.getByLabelText('CREATE TABLE statements'))
    await user.keyboard('{Control>}{Enter}{/Control}')

    expect(props.onGenerate).toHaveBeenCalledOnce()
  })

  it('does not generate from blank input with Ctrl+Enter', async () => {
    const user = userEvent.setup()
    const props = renderEditor({ sql: '   ' })

    await user.click(screen.getByLabelText('CREATE TABLE statements'))
    await user.keyboard('{Control>}{Enter}{/Control}')

    expect(props.onGenerate).not.toHaveBeenCalled()
  })

  it('shows and disables controls during generation', () => {
    renderEditor({
      sql: 'CREATE TABLE users ();',
      loading: true,
      canClear: true,
    })

    expect(screen.getByLabelText('CREATE TABLE statements')).toHaveAttribute(
      'aria-busy',
      'true',
    )
    expect(screen.getByRole('button', { name: 'Generating…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Load Example' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Clear' })).toBeDisabled()
  })
})
