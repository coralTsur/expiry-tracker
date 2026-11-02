import { CATEGORY_LABELS } from '../utils/categoryLabels';
import { formatDate } from '../utils/formatDate';

function ItemRow({ item }) {
  return (
    <tr>
      <td>{CATEGORY_LABELS[item.category] ?? item.category}</td>
      <td>{item.name}</td>
      <td>{formatDate(item.expirationDate)}</td>
      <td>{item.redeemed ? 'כן' : 'לא'}</td>
    </tr>
  );
}

export default ItemRow;
