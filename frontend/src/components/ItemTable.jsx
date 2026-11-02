import ItemRow from './ItemRow';
import './ItemTable.css';

function ItemTable({ items }) {
  return (
    <div className="item-table-wrapper">
      <table className="item-table">
        <thead>
          <tr>
            <th>קטגוריה</th>
            <th>שם</th>
            <th>תאריך תוקף</th>
            <th>מומש</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <ItemRow key={item.id} item={item} />
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ItemTable;
