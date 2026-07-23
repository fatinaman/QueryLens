import type {
  ApiErrorResponse,
  ColumnDto,
  ForeignKeyDto,
  SchemaParseRequest,
  SchemaParseResponse,
  TableDto,
} from '../types/schema'

const FALLBACK_API_BASE_URL = 'http://localhost:8080'

const configuredBaseUrl =
  import.meta.env.VITE_API_BASE_URL?.trim() || FALLBACK_API_BASE_URL

const API_BASE_URL = configuredBaseUrl.replace(/\/+$/, '')

export class SchemaApiError extends Error {
  readonly details: string[]
  readonly status?: number

  constructor(message: string, details: string[] = [], status?: number) {
    super(message)
    this.name = 'SchemaApiError'
    this.details = details
    this.status = status
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  return (
    isRecord(value) &&
    typeof value.message === 'string' &&
    isStringArray(value.errors)
  )
}

function isColumn(value: unknown): value is ColumnDto {
  return (
    isRecord(value) &&
    typeof value.name === 'string' &&
    typeof value.dataType === 'string' &&
    typeof value.nullable === 'boolean' &&
    typeof value.primaryKey === 'boolean'
  )
}

function isForeignKey(value: unknown): value is ForeignKeyDto {
  return (
    isRecord(value) &&
    typeof value.column === 'string' &&
    typeof value.referencedTable === 'string' &&
    typeof value.referencedColumn === 'string'
  )
}

function isTable(value: unknown): value is TableDto {
  return (
    isRecord(value) &&
    typeof value.name === 'string' &&
    Array.isArray(value.columns) &&
    value.columns.every(isColumn) &&
    Array.isArray(value.foreignKeys) &&
    value.foreignKeys.every(isForeignKey)
  )
}

function isSchemaParseResponse(value: unknown): value is SchemaParseResponse {
  return (
    isRecord(value) &&
    Array.isArray(value.tables) &&
    value.tables.every(isTable)
  )
}

async function readJson(response: Response): Promise<unknown> {
  const body = await response.text()

  if (!body) {
    throw new SchemaApiError(
      'Unable to generate the diagram',
      ['The backend returned an empty response.'],
      response.status,
    )
  }

  try {
    return JSON.parse(body)
  } catch {
    throw new SchemaApiError(
      'Unable to generate the diagram',
      ['The backend returned an invalid response.'],
      response.status,
    )
  }
}

export async function parseSchema(
  sql: string,
  signal?: AbortSignal,
): Promise<SchemaParseResponse> {
  const request: SchemaParseRequest = { sql }

  try {
    const response = await fetch(`${API_BASE_URL}/api/schema/parse`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
      signal,
    })

    const body = await readJson(response)

    if (!response.ok) {
      if (isApiErrorResponse(body)) {
        throw new SchemaApiError(body.message, body.errors, response.status)
      }

      throw new SchemaApiError(
        'Unable to generate the diagram',
        ['The backend could not process the request.'],
        response.status,
      )
    }

    if (!isSchemaParseResponse(body)) {
      throw new SchemaApiError(
        'Unable to generate the diagram',
        ['The backend returned an unexpected response shape.'],
        response.status,
      )
    }

    return body
  } catch (error) {
    if (error instanceof SchemaApiError) {
      throw error
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }

    throw new SchemaApiError(
      'Unable to connect to the QueryLens backend.',
      ['Make sure the backend is running and the API URL is configured correctly.'],
    )
  }
}
