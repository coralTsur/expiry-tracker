import { useEffect, useState } from 'react';
import { fetchItems } from '../api/itemsApi';
import ItemTable from './ItemTable';

function ItemList() {
  const [items, setItems] = useState([]);
  // Starts as true so the first render already shows the loading text.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  if (loading) {
    return <p role="status">טוען פריטים...</p>;
  }

  if (error) {
    return <p role="alert">{error.message}</p>;
  }

  if (items.length === 0) {
    return <p>אין עדיין פריטים.</p>;
  }

  return <ItemTable items={items} />;
}

export default ItemList;
