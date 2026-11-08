import { useEffect, useState } from 'react';
import { deleteItem, redeemItem } from '../api/itemsApi';
import ItemTable from './ItemTable';

// loadItems: the API function that fetches this screen's list (fetchItems or fetchExpiringSoon).
// removeRedeemed: true on lists that must not show redeemed items, so a redeemed row is removed.
function ItemList({ loadItems, emptyText, removeRedeemed }) {
  const [items, setItems] = useState([]);
  // Starts as true so the first render already shows the loading text.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // Errors from redeem/delete. Shown above the table, so the list stays visible.
  const [actionError, setActionError] = useState(null);
  // True while a redeem or delete request is running. Disables every action button.
  const [actionPending, setActionPending] = useState(false);

  useEffect(() => {
    // Named load, not loadItems, so it does not hide the loadItems prop.
    async function load() {
      try {
        const data = await loadItems();
        setItems(data);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    }

    load();
    // The pages pass module-level API functions, so loadItems keeps the same reference
    // between renders and the effect runs only once per mount.
  }, [loadItems]);

  async function handleRedeem(item) {
    setActionError(null);
    setActionPending(true);
    try {
      const updatedItem = await redeemItem(item.id);
      // Functional update: after the await, the items from this render may be stale.
      if (removeRedeemed) {
        setItems((prevItems) => prevItems.filter((i) => i.id !== updatedItem.id));
      } else {
        setItems((prevItems) =>
          prevItems.map((i) => (i.id === updatedItem.id ? updatedItem : i)),
        );
      }
    } catch (err) {
      setActionError(err.message);
    } finally {
      setActionPending(false);
    }
  }

  async function handleDelete(item) {
    if (!window.confirm(`למחוק את "${item.name}"?`)) {
      return;
    }

    setActionError(null);
    setActionPending(true);
    try {
      await deleteItem(item.id);
      setItems((prevItems) => prevItems.filter((i) => i.id !== item.id));
    } catch (err) {
      // 404: the item is already gone on the server, so remove the row as well.
      if (err.status === 404) {
        setItems((prevItems) => prevItems.filter((i) => i.id !== item.id));
      }
      setActionError(err.message);
    } finally {
      setActionPending(false);
    }
  }

  if (loading) {
    return <p role="status">טוען פריטים...</p>;
  }

  if (error) {
    return <p role="alert">{error.message}</p>;
  }

  return (
    <>
      {actionError && <p role="alert">{actionError}</p>}
      {items.length === 0 ? (
        <p>{emptyText}</p>
      ) : (
        <ItemTable
          items={items}
          onRedeem={handleRedeem}
          onDelete={handleDelete}
          actionPending={actionPending}
        />
      )}
    </>
  );
}

export default ItemList;
