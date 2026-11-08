import { Link } from 'react-router';

function NotFoundPage() {
  return (
    <>
      <title>הדף לא נמצא | מעקב אחרי פריטים עם תוקף</title>
      <h1>הדף לא נמצא</h1>
      <Link to="/">חזרה לכל הפריטים</Link>
    </>
  );
}

export default NotFoundPage;
