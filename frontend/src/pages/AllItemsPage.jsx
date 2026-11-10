import { useLocation } from 'react-router';
import { fetchItems } from '../api/itemsApi';
import ItemList from '../components/ItemList';

function AllItemsPage() {
  const location = useLocation();
  // Read here and not in ItemList: only "/" receives a message from add and edit,
  // and ItemList stays unaware of routing.
  const initialSuccessMessage = location.state?.message ?? null;

  return (
    <>
      {/* React 19 moves <title> into <head> automatically. */}
      <title>כל הפריטים | מעקב אחרי פריטים עם תוקף</title>
      <h1>כל הפריטים</h1>
      <ItemList
        loadItems={fetchItems}
        emptyText="אין עדיין פריטים."
        removeRedeemed={false}
        initialSuccessMessage={initialSuccessMessage}
      />
    </>
  );
}

export default AllItemsPage;
