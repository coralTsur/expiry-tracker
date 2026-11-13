import ItemRow from './ItemRow';
import './ItemTable.css';

function ItemTable({ items, onRedeem, onDelete, actionPending }) {
  return (
    <div className="item-table-wrapper">
      {/* Explicit roles: on phones the CSS shows rows as cards (display: grid), and some browsers
          then drop the native table semantics. The roles keep it a table for screen readers. */}
      <table className="item-table" role="table">
        <thead role="rowgroup">
          <tr role="row">
            <th role="columnheader">קטגוריה</th>
            <th role="columnheader">שם</th>
            <th role="columnheader">תאריך תוקף</th>
            <th role="columnheader">סטטוס</th>
            {/*<th>מומש</th>*/}
            <th role="columnheader">פעולות</th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          {items.map((item) => (
            <ItemRow
              key={item.id}
              item={item}
              onRedeem={onRedeem}
              onDelete={onDelete}
              actionPending={actionPending}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ItemTable;
