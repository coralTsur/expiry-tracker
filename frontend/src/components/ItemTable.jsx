import ItemRow from './ItemRow';
import './ItemTable.css';

function ItemTable({ items, onRedeem, onDelete, actionPending }) {
  return (
    <div className="item-table-wrapper">
      <table className="item-table">
        <thead>
          <tr>
            <th>קטגוריה</th>
            <th>שם</th>
            <th>תאריך תוקף</th>
            <th>סטטוס</th>
            {/*<th>מומש</th>*/}
            <th>פעולות</th>
          </tr>
        </thead>
        <tbody>
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
