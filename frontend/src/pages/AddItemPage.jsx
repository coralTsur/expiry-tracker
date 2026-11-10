import { useNavigate } from 'react-router';
import { createItem } from '../api/itemsApi';
import ItemForm from '../components/ItemForm';
import { EMPTY_ITEM_VALUES } from '../utils/itemForm';

function AddItemPage() {
  const navigate = useNavigate();

  // No try/catch here: an error goes back to ItemForm, which shows it next to the fields.
  async function handleSubmit(item) {
    const savedItem = await createItem(item);
    // The name comes from the item the server saved. The message travels in the navigation state,
    // so the list screen can show it after it mounts.
    navigate('/', { state: { message: `הפריט "${savedItem.name}" נוסף` } });
  }

  return (
    <>
      {/* React 19 moves <title> into <head> automatically. */}
      <title>הוספת פריט | מעקב אחרי פריטים עם תוקף</title>
      <h1>הוספת פריט</h1>
      <ItemForm initialValues={EMPTY_ITEM_VALUES} submitLabel="הוסף" onSubmit={handleSubmit} />
    </>
  );
}

export default AddItemPage;
