import { NavLink, Outlet } from 'react-router';
import './Layout.css';

function Layout() {
  return (
    <>
      <nav aria-label="ניווט ראשי" className="main-nav">
        <ul>
          <li>
            {/* end: without it, "/" would match every path and always look active. */}
            <NavLink to="/" end>
              כל הפריטים
            </NavLink>
          </li>
          <li>
            <NavLink to="/expiring-soon">פג בקרוב</NavLink>
          </li>
          <li>
            <NavLink to="/items/new">הוספת פריט</NavLink>
          </li>
        </ul>
      </nav>
      <main>
        <Outlet />
      </main>
    </>
  );
}

export default Layout;
