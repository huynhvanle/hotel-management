import { useState, useEffect } from 'react';
import { Users, UserCircle, Building2, BedDouble, CalendarCheck, TrendingUp, TrendingDown, DollarSign } from 'lucide-react';
import { userService, clientService } from '../services/api';
import './Dashboard.css';

export default function Dashboard() {
  const [stats, setStats] = useState({
    users: 0,
    clients: 0,
    hotels: 0,
    rooms: 0,
    bookings: 0,
  });
  const [recentUsers, setRecentUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [usersRes] = await Promise.allSettled([
        userService.getAll(),
      ]);
      if (usersRes.status === 'fulfilled') {
        const users = usersRes.value.data?.users || [];
        setStats(prev => ({ ...prev, users: users.length }));
        setRecentUsers(users.slice(0, 5));
      }
    } catch (error) {
      console.log('Error fetching stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    { label: 'Người dùng', value: stats.users, icon: Users, color: '#3b82f6', bg: '#eff6ff' },
    { label: 'Khách hàng', value: stats.clients, icon: UserCircle, color: '#10b981', bg: '#ecfdf5' },
    { label: 'Khách sạn', value: stats.hotels, icon: Building2, color: '#f59e0b', bg: '#fffbeb' },
    { label: 'Phòng', value: stats.rooms, icon: BedDouble, color: '#8b5cf6', bg: '#f5f3ff' },
    { label: 'Đặt phòng', value: stats.bookings, icon: CalendarCheck, color: '#ef4444', bg: '#fef2f2' },
  ];

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div>
          <h2>Chào mừng quay trở lại!</h2>
          <p>Đây là tổng quan về hệ thống quản lý khách sạn của bạn</p>
        </div>
      </div>

      <div className="stats-grid">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="stat-card" style={{ '--stat-color': stat.color, '--stat-bg': stat.bg }}>
              <div className="stat-icon" style={{ background: stat.bg, color: stat.color }}>
                <Icon size={24} />
              </div>
              <div className="stat-info">
                <span className="stat-value">{loading ? '...' : stat.value}</span>
                <span className="stat-label">{stat.label}</span>
              </div>
            </div>
          );
        })}
      </div>

      <div className="dashboard-grid">
        <div className="card">
          <div className="card-header">
            <h3>Người dùng gần đây</h3>
          </div>
          <div className="card-body">
            {loading ? (
              <div className="loading-text">Đang tải...</div>
            ) : recentUsers.length === 0 ? (
              <div className="empty-text">Chưa có người dùng nào</div>
            ) : (
              <div className="user-list">
                {recentUsers.map((user) => (
                  <div key={user.id} className="user-item">
                    <div className="user-avatar-sm">
                      {user.fullName?.charAt(0)?.toUpperCase() || 'U'}
                    </div>
                    <div className="user-info-sm">
                      <span className="user-name-sm">{user.fullName}</span>
                      <span className="user-role-sm">{user.position || 'Nhân viên'}</span>
                    </div>
                    <div className="user-badge">{user.mail}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h3>Hoạt động nhanh</h3>
          </div>
          <div className="card-body">
            <div className="quick-actions">
              <button className="quick-action-btn">
                <Users size={20} />
                <span>Thêm người dùng</span>
              </button>
              <button className="quick-action-btn">
                <UserCircle size={20} />
                <span>Thêm khách hàng</span>
              </button>
              <button className="quick-action-btn">
                <BedDouble size={20} />
                <span>Đặt phòng mới</span>
              </button>
              <button className="quick-action-btn">
                <CalendarCheck size={20} />
                <span>Xem đặt phòng</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
