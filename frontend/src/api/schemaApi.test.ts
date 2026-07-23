import { beforeEach, describe, expect, it, vi } from 'vitest'
import { parseSchema, SchemaApiError } from './schemaApi'
import { schemaResponse } from '../test/fixtures'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('parseSchema', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })

  it('posts SQL to the schema endpoint and returns a validated response', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(schemaResponse))
    const controller = new AbortController()

    await expect(parseSchema('CREATE TABLE users ();', controller.signal))
      .resolves.toEqual(schemaResponse)

    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8080/api/schema/parse',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql: 'CREATE TABLE users ();' }),
        signal: controller.signal,
      },
    )
  })

  it('preserves structured backend errors and status codes', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({
      message: 'Unable to parse SQL schema',
      errors: ['Only CREATE TABLE statements are supported'],
    }, 400))

    const error = await parseSchema('DROP TABLE users').catch(
      (reason: unknown) => reason,
    )

    expect(error).toBeInstanceOf(SchemaApiError)
    expect(error).toMatchObject({
      message: 'Unable to parse SQL schema',
      details: ['Only CREATE TABLE statements are supported'],
      status: 400,
    })
  })

  it('handles unstructured error responses safely', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ error: 'nope' }, 500))

    await expect(parseSchema('CREATE TABLE users ();')).rejects.toMatchObject({
      message: 'Unable to generate the diagram',
      details: ['The backend could not process the request.'],
      status: 500,
    })
  })

  it('rejects empty server responses', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('', { status: 200 }))

    await expect(parseSchema('CREATE TABLE users ();')).rejects.toMatchObject({
      message: 'Unable to generate the diagram',
      details: ['The backend returned an empty response.'],
    })
  })

  it('rejects non-JSON server responses', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('<html>failure</html>', {
      status: 502,
    }))

    await expect(parseSchema('CREATE TABLE users ();')).rejects.toMatchObject({
      message: 'Unable to generate the diagram',
      details: ['The backend returned an invalid response.'],
      status: 502,
    })
  })

  it('rejects successful responses with an invalid contract', async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({
      tables: [{ name: 'users', columns: 'invalid', foreignKeys: [] }],
    }))

    await expect(parseSchema('CREATE TABLE users ();')).rejects.toMatchObject({
      message: 'Unable to generate the diagram',
      details: ['The backend returned an unexpected response shape.'],
    })
  })

  it('converts network failures into a safe application error', async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(parseSchema('CREATE TABLE users ();')).rejects.toMatchObject({
      message: 'Unable to connect to the QueryLens backend.',
      details: [
        'Make sure the backend is running and the API URL is configured correctly.',
      ],
    })
  })

  it('preserves AbortError so callers can ignore cancelled requests', async () => {
    const abortError = new DOMException('Request aborted', 'AbortError')
    vi.mocked(fetch).mockRejectedValue(abortError)

    await expect(parseSchema('CREATE TABLE users ();')).rejects.toBe(abortError)
  })
})
