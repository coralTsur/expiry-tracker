import { Route, Routes } from 'react-router';
import Layout from './components/Layout';
import AddItemPage from './pages/AddItemPage';
import AllItemsPage from './pages/AllItemsPage';
import EditItemPage from './pages/EditItemPage';
import ExpiringSoonPage from './pages/ExpiringSoonPage';
import NotFoundPage from './pages/NotFoundPage';

function App() {
  return (
    <Routes>
      {/* Layout route: no path of its own, it wraps every screen with the nav. */}
      <Route element={<Layout />}>
        <Route index element={<AllItemsPage />} />
        <Route path="expiring-soon" element={<ExpiringSoonPage />} />
        <Route path="items/new" element={<AddItemPage />} />
        <Route path="items/:id/edit" element={<EditItemPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

export default App;
