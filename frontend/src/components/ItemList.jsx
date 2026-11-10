import { useEffect, useState } from 'react';
import { deleteItem, redeemItem } from '../api/itemsApi';
import ItemTable from './ItemTable';

// How long a success message stays on screen (spec section 9, "הודעות אחרי פעולה").
const SUCCESS_MESSAGE_MS = 5000;

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

  // Hides a success message after SUCCESS_MESSAGE_MS. Errors get no timer.
  useEffect(() => {
    // Start only after the list has loaded, so a message from add or edit
    // (which arrives while the list is still loading) gets its full 5 seconds on screen.
    if (!successMessage || loading) {
      return;
    }

    const timerId = setTimeout(() => setSuccessMessage(null), SUCCESS_MESSAGE_MS);
    // The cleanup clears the old timer when the message changes (a new message restarts
    // the 5 seconds) and when the component unmounts (screen change). So setState never
    // runs on an unmounted component, and an old timer never hides a newer message.
    // Two messages with the same text also restart the timer: each action first sets the
    // message to null, and the await gives a render in between, so this effect runs again.
    return () => clearTimeout(timerId);
  }, [successMessage, loading]);

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
