import React from "react";
import "./Table.css";

export interface ITableColumn<T> {
  key: keyof T;
  label: string;
  render?: (row: T) => React.ReactNode;
}

interface TableProps<T> {
  columns: ITableColumn<T>[];
  data: T[];
  actions?: (row: T) => React.ReactNode;
}

function Table<T extends { id: number }>({
  columns,
  data,
  actions,
}: TableProps<T>) {
  return (
    <div className="table-outer-scroll">
      <table className="table-root">
        <thead>
          <tr className="table-header">
            {columns.map((col) => (
              <th key={String(col.key)} className={`table-col table-col-${String(col.key)}`} scope="col">
                {col.label}
              </th>
            ))}
            {actions && <th className="table-col table-col-actions">Actions</th>}
          </tr>
        </thead>
        <tbody className="table-container">
          {data.map((row) => (
            <tr className="table-row" key={row.id}>
              {columns.map((col) => (
                <td key={String(col.key)} className={`table-col table-col-${String(col.key)}`}>
                  {col.render ? col.render(row) : (row[col.key] as React.ReactNode)}
                </td>
              ))}
              {actions && (
                <td className="table-col table-col-actions">{actions(row)}</td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Table;
