import { Link } from 'react-router';
import { CATEGORY_LABELS } from '../utils/categoryLabels';
import { formatDate } from '../utils/formatDate';
import { getItemStatus, STATUS_LABELS } from '../utils/itemStatus';
import { CheckIcon, PencilIcon, TrashIcon } from './Icons';

// One CSS modifier per status, for the pill colour (ItemTable.css).
const STATUS_CLASS_NAMES = {
  REDEEMED: 'status-redeemed',
  EXPIRED: 'status-expired',
  EXPIRING_SOON: 'status-expiring',
  VALID: 'status-valid',
};

// spec section 9, row actions: icon-only. The original text is the accessible name (aria-label),
// so screen readers still say "עריכה" etc., and the tooltip (title) for mouse users.
function ItemRow({ item, onRedeem, onDelete, actionPending }) {
  const status = getItemStatus(item);

  // role="row" and role="cell": see ItemTable. The cell-* class names place each cell in the phone card (ItemTable.css).
  return (
    <tr role="row">
      <td role="cell" className="cell-category">{CATEGORY_LABELS[item.category] ?? item.category}</td>
      <td role="cell" className="cell-name">{item.name}</td>
      <td role="cell" className="cell-date">{formatDate(item.expirationDate)}</td>
      {/* spec section 9: the status is text with a colour, not a colour alone. */}
      <td role="cell" className="cell-status">
        <span className={`status ${STATUS_CLASS_NAMES[status]}`}>{STATUS_LABELS[status]}</span>
      </td>
      {
      //<td>{item.redeemed ? 'כן' : 'לא'}</td>
      }
      <td role="cell" className="cell-actions">
        <div className="item-actions">
          {/* A link, not a button: it moves to another screen and changes nothing by itself. */}
          <Link
            to={`/items/${item.id}/edit`}
            className="button-link icon-button"
            aria-label="עריכה"
            title="עריכה"
          >
            <PencilIcon />
          </Link>
          {!item.redeemed && (
            <button
              type="button"
              className="icon-button"
              aria-label="סימון כמומש"
              title="סימון כמומש"
              disabled={actionPending}
              onClick={() => onRedeem(item)}
            >
              <CheckIcon />
            </button>
          )}
          <button
            type="button"
            className="button-danger icon-button"
            aria-label="מחיקה"
            title="מחיקה"
            disabled={actionPending}
            onClick={() => onDelete(item)}
          >
            <TrashIcon />
          </button>
        </div>
      </td>
    </tr>
  );
}

export default ItemRow;
