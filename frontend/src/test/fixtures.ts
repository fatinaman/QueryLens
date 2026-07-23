import type { SchemaParseResponse, TableDto } from '../types/schema'

export const usersTable: TableDto = {
  name: 'users',
  columns: [
    {
      name: 'id',
      dataType: 'BIGSERIAL',
      nullable: false,
      primaryKey: true,
    },
    {
      name: 'email',
      dataType: 'VARCHAR(255)',
      nullable: false,
      primaryKey: false,
    },
  ],
  foreignKeys: [],
}

export const projectsTable: TableDto = {
  name: 'projects',
  columns: [
    {
      name: 'id',
      dataType: 'BIGSERIAL',
      nullable: false,
      primaryKey: true,
    },
    {
      name: 'owner_id',
      dataType: 'BIGINT',
      nullable: true,
      primaryKey: false,
    },
  ],
  foreignKeys: [
    {
      column: 'owner_id',
      referencedTable: 'users',
      referencedColumn: 'id',
    },
  ],
}

export const schemaResponse: SchemaParseResponse = {
  tables: [usersTable, projectsTable],
}
