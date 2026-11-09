import { useNavigate } from 'react-router';
import { createItem } from '../api/itemsApi';
import ItemForm from '../components/ItemForm';
import { EMPTY_ITEM_VALUES } from '../utils/itemForm';

function AddItemPage() {
  const navigate = useNavigate();

  // No try/catch here: an error goes back to ItemForm, which shows it next to the fields.
  async function handleSubmit(item) {
    await createItem(item);
    // No success message. The list loads the new item when it mounts.
    navigate('/');
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
