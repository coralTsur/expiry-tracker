import { Link } from 'react-router';
import { CATEGORY_LABELS } from '../utils/categoryLabels';
import { formatDate } from '../utils/formatDate';
import { CheckIcon, PencilIcon, TrashIcon } from './Icons';

// spec section 9, row actions: icon-only. The original text is the accessible name (aria-label),
// so screen readers still say "עריכה" etc., and the tooltip (title) for mouse users.
function ItemRow({ item, onRedeem, onDelete, actionPending }) {
  return (
    <tr>
      <td>{CATEGORY_LABELS[item.category] ?? item.category}</td>
      <td>{item.name}</td>
      <td>{formatDate(item.expirationDate)}</td>
      <td>{item.redeemed ? 'כן' : 'לא'}</td>
      <td>
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
