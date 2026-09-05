import { NavLink, Route, Routes } from 'react-router-dom';
import CustomersPage from './pages/CustomersPage';
import SettingsPage from './pages/SettingsPage';
import RestaurantsPage from './pages/RestaurantsPage';
import OrdersPage from './pages/OrdersPage';
import DataPage from './pages/DataPage';

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>SwiftEats Admin</h1>
        <nav>
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/restaurants">Restaurants</NavLink>
          <div className="sidebar-subnav">
            <NavLink to="/restaurants#create">Create</NavLink>
            <NavLink to="/restaurants#approve">Approve</NavLink>
            <NavLink to="/restaurants#add-item">Add item</NavLink>
            <NavLink to="/restaurants#update">Update</NavLink>
            <NavLink to="/restaurants#delete">Delete</NavLink>
          </div>
          <NavLink to="/orders">Orders</NavLink>
          <NavLink to="/customers">Customers</NavLink>
          <NavLink to="/data">Analytics Data</NavLink>
          <NavLink to="/settings">Settings</NavLink>
        </nav>
      </aside>
      <main className="main">
        <Routes>
          <Route path="/" element={<SettingsPage overview />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/restaurants" element={<RestaurantsPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/customers" element={<CustomersPage />} />
          <Route path="/data" element={<DataPage />} />
        </Routes>
      </main>
    </div>
  );
}
