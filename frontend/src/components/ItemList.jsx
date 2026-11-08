import { useEffect, useState } from 'react';
import { deleteItem, fetchItems, redeemItem } from '../api/itemsApi';
import ItemTable from './ItemTable';

function ItemList() {
  const [items, setItems] = useState([]);
  // Starts as true so the first render already shows the loading text.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // Errors from redeem/delete. Shown above the table, so the list stays visible.
  const [actionError, setActionError] = useState(null);
  // True while a redeem or delete request is running. Disables every action button.
  const [actionPending, setActionPending] = useState(false);

  useEffect(() => {
    async function loadItems() {
      try {
        const data = await fetchItems();
        setItems(data);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    }

    loadItems();
  }, []);

  async function handleRedeem(item) {
    setActionError(null);
    setActionPending(true);
    try {
      const updatedItem = await redeemItem(item.id);
      // Functional update: after the await, the items from this render may be stale.
      setItems((prevItems) =>
        prevItems.map((i) => (i.id === updatedItem.id ? updatedItem : i)),
      );
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
        <p>אין עדיין פריטים.</p>
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
