import { fetchExpiringSoon } from '../api/itemsApi';
import ItemList from '../components/ItemList';

function ExpiringSoonPage() {
  return (
    <>
      {/* React 19 moves <title> into <head> automatically. */}
      <title>פג בקרוב | מעקב אחרי פריטים עם תוקף</title>
      <h1>פג בקרוב</h1>
      {/* removeRedeemed: a redeemed item does not belong on this list (spec section 5). */}
      <ItemList loadItems={fetchExpiringSoon} emptyText="אין פריטים שתוקפם מתקרב." removeRedeemed={true} />
    </>
  );
}

export default ExpiringSoonPage;
