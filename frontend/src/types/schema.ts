export interface ColumnDto {
  name: string
  dataType: string
  nullable: boolean
  primaryKey: boolean
}

export interface ForeignKeyDto {
  column: string
  referencedTable: string
  referencedColumn: string
}

export interface TableDto {
  name: string
  columns: ColumnDto[]
  foreignKeys: ForeignKeyDto[]
}

export interface SchemaParseResponse {
  tables: TableDto[]
}

export interface ApiErrorResponse {
  message: string
  errors: string[]
}

export interface SchemaParseRequest {
  sql: string
}
