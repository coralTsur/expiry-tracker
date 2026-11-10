import { useEffect, useState } from 'react';
import { deleteItem, redeemItem } from '../api/itemsApi';
import ItemTable from './ItemTable';

// loadItems: the API function that fetches this screen's list (fetchItems or fetchExpiringSoon).
// removeRedeemed: true on lists that must not show redeemed items, so a redeemed row is removed.
// initialSuccessMessage: a success message to show on mount (after add or edit), or null.
function ItemList({ loadItems, emptyText, removeRedeemed, initialSuccessMessage }) {
  const [items, setItems] = useState([]);
  // Starts as true so the first render already shows the loading text.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // Errors from redeem/delete. Shown above the table, so the list stays visible.
  const [actionError, setActionError] = useState(null);
  // useState reads the initial value only on mount. The list page mounts fresh after
  // navigate('/'), so it picks up the message, and from then on ItemList owns it.
  const [successMessage, setSuccessMessage] = useState(initialSuccessMessage ?? null);
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
    setSuccessMessage(null);
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
      setSuccessMessage(`הפריט "${item.name}" סומן כמומש`);
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

    // Cleared only after the confirm, so Cancel leaves the current message as it is.
    setActionError(null);
    setSuccessMessage(null);
    setActionPending(true);
    try {
      await deleteItem(item.id);
      setItems((prevItems) => prevItems.filter((i) => i.id !== item.id));
      setSuccessMessage(`הפריט "${item.name}" נמחק`);
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
    return <p className="loading" role="status">טוען פריטים...</p>;
  }

  if (error) {
    return <p className="alert" role="alert">{error.message}</p>;
  }

  return (
    <>
      {actionError && <p className="alert" role="alert">{actionError}</p>}
      {/* Always mounted, so screen readers announce the text when it appears inside. */}
      <div role="status">
        {successMessage && <p className="success">{successMessage}</p>}
      </div>
      {items.length === 0 ? (
        <p className="empty-state">{emptyText}</p>
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
