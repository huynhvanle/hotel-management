import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  UserCircle,
  Building2,
  BedDouble,
  CalendarCheck,
  LogOut,
  Menu,
  X,
} from 'lucide-react';
import './Layout.css';

const menuItems = [
  { path: '/', label: 'Tổng quan', icon: LayoutDashboard },
  { path: '/users', label: 'Người dùng', icon: Users },
  { path: '/clients', label: 'Khách hàng', icon: UserCircle },
  { path: '/hotels', label: 'Khách sạn', icon: Building2 },
  { path: '/rooms', label: 'Phòng', icon: BedDouble },
  { path: '/bookings', label: 'Đặt phòng', icon: CalendarCheck },
];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="layout">
      <aside className={`sidebar ${sidebarOpen ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          <div className="logo">
            <Building2 size={28} />
            <span className="logo-text">HotelPro</span>
          </div>
          <button className="toggle-btn" onClick={() => setSidebarOpen(!sidebarOpen)}>
            {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        <nav className="sidebar-nav">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <button
                key={item.path}
                className={`nav-item ${isActive ? 'active' : ''}`}
                onClick={() => navigate(item.path)}
              >
                <Icon size={20} />
                {sidebarOpen && <span>{item.label}</span>}
              </button>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">
              {user?.fullName?.charAt(0)?.toUpperCase() || 'U'}
            </div>
            {sidebarOpen && (
              <div className="user-details">
                <span className="user-name">{user?.fullName || 'User'}</span>
                <span className="user-role">{user?.position || 'Staff'}</span>
              </div>
            )}
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            <LogOut size={18} />
            {sidebarOpen && <span>Đăng xuất</span>}
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="top-bar">
          <div className="page-title">
            {menuItems.find((item) => item.path === location.pathname)?.label || 'Trang chủ'}
          </div>
          <div className="top-bar-right">
            <span className="user-welcome">Xin chào, {user?.fullName || 'User'}</span>
          </div>
        </header>
        <div className="content-area">{children}</div>
      </main>
    </div>
  );
}
