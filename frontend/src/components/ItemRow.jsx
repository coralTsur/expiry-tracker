import { CATEGORY_LABELS } from '../utils/categoryLabels';
import { formatDate } from '../utils/formatDate';

function ItemRow({ item, onRedeem, onDelete, actionPending }) {
  return (
    <tr>
      <td>{CATEGORY_LABELS[item.category] ?? item.category}</td>
      <td>{item.name}</td>
      <td>{formatDate(item.expirationDate)}</td>
      <td>{item.redeemed ? 'כן' : 'לא'}</td>
      <td>
        <div className="item-actions">
          {!item.redeemed && (
            <button
              type="button"
              disabled={actionPending}
              onClick={() => onRedeem(item)}
            >
              סימון כמומש
            </button>
          )}
          <button
            type="button"
            disabled={actionPending}
            onClick={() => onDelete(item)}
          >
            מחיקה
          </button>
        </div>
      </td>
    </tr>
  );
}

export default ItemRow;
