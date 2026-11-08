import { fetchItems } from '../api/itemsApi';
import ItemList from '../components/ItemList';

function AllItemsPage() {
  return (
    <>
      {/* React 19 moves <title> into <head> automatically. */}
      <title>כל הפריטים | מעקב אחרי פריטים עם תוקף</title>
      <h1>כל הפריטים</h1>
      <ItemList loadItems={fetchItems} emptyText="אין עדיין פריטים." removeRedeemed={false} />
    </>
  );
}

export default AllItemsPage;
