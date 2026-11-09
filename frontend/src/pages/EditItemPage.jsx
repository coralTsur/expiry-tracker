import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { fetchItem, NOT_FOUND_MESSAGE, updateItem } from '../api/itemsApi';
import ItemForm from '../components/ItemForm';
import { toFormValues } from '../utils/itemForm';

// The server's id is a whole number. Anything else (for example /items/abc/edit) cannot be an item,
// and the server would answer it with a 400 that does not describe the problem.
const ID_PATTERN = /^[0-9]+$/;

function EditItemPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  // Starts as true so the first render already shows the loading text.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const validId = ID_PATTERN.test(id);

  useEffect(() => {
    // No request for an id that is not a number. The render below shows the not-found message.
    if (!ID_PATTERN.test(id)) {
      return;
    }

    async function load() {
      try {
        const data = await fetchItem(id);
        setItem(data);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [id]);

  // No try/catch here: an error goes back to ItemForm, which shows it next to the fields.
  async function handleSubmit(body) {
    await updateItem(id, body);
    // No success message. The list loads the updated item when it mounts.
    navigate('/');
  }

  function renderLoadError(message) {
    return (
      <>
        <p role="alert">{message}</p>
        <Link to="/">חזרה לכל הפריטים</Link>
      </>
    );
  }

  let content;
  if (!validId) {
    content = renderLoadError(NOT_FOUND_MESSAGE);
  } else if (loading) {
    content = <p role="status">טוען פריט...</p>;
  } else if (error) {
    // 404 gets the fixed not-found text from request(), a network or server error its own message.
    content = renderLoadError(error.message);
  } else {
    // Rendered only after the item arrived: ItemForm's useState reads initialValues on its first render only.
    content = (
      <ItemForm
        initialValues={toFormValues(item)}
        submitLabel="שמור"
        onSubmit={handleSubmit}
        showRedeemed={true}
      />
    );
  }

  return (
    <>
      {/* React 19 moves <title> into <head> automatically. */}
      <title>עריכת פריט | מעקב אחרי פריטים עם תוקף</title>
      <h1>עריכת פריט</h1>
      {content}
    </>
  );
}

export default EditItemPage;
