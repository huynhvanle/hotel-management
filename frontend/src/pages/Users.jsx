import { useState, useEffect } from 'react';
import { Plus, Search, Edit2, Trash2, X } from 'lucide-react';
import { userService } from '../services/api';
import './Users.css';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editUser, setEditUser] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    fullName: '',
    email: '',
    position: '',
    description: '',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await userService.getAll();
      setUsers(response.data?.users || []);
    } catch (err) {
      console.error('Error fetching users:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (user = null) => {
    if (user) {
      setEditUser(user);
      setFormData({
        username: user.username || '',
        password: '',
        fullName: user.fullName || '',
        email: user.mail || '',
        position: user.position || '',
        description: user.description || '',
      });
    } else {
      setEditUser(null);
      setFormData({ username: '', password: '', fullName: '', email: '', position: '', description: '' });
    }
    setError('');
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditUser(null);
    setError('');
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editUser) {
        const updateData = { ...formData };
        if (!updateData.password) delete updateData.password;
        await userService.update(editUser.id, updateData);
      } else {
        await userService.create(formData);
      }
      handleCloseModal();
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Có lỗi xảy ra');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa người dùng này?')) {
      try {
        await userService.delete(id);
        fetchUsers();
      } catch (err) {
        console.error('Error deleting user:', err);
      }
    }
  };

  const filteredUsers = users.filter(
    (u) =>
      (u.fullName?.toLowerCase() || '').includes(searchTerm.toLowerCase()) ||
      (u.username?.toLowerCase() || '').includes(searchTerm.toLowerCase()) ||
      (u.mail?.toLowerCase() || '').includes(searchTerm.toLowerCase())
  );

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>Quản lý Người dùng</h2>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={18} />
          Thêm người dùng
        </button>
      </div>

      <div className="search-bar">
        <Search size={18} />
        <input
          type="text"
          placeholder="Tìm kiếm theo tên, username, email..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      <div className="table-container">
        {loading ? (
          <div className="loading-text">Đang tải dữ liệu...</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Họ tên</th>
                <th>Tên đăng nhập</th>
                <th>Email</th>
                <th>Chức vụ</th>
                <th>Mô tả</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="7" className="empty-row">Không có dữ liệu</td>
                </tr>
              ) : (
                filteredUsers.map((user, index) => (
                  <tr key={user.id}>
                    <td>{index + 1}</td>
                    <td className="name-cell">{user.fullName || '-'}</td>
                    <td>{user.username || '-'}</td>
                    <td>{user.mail || '-'}</td>
                    <td>
                      <span className={`badge badge-${(user.position || 'staff').toLowerCase()}`}>
                        {user.position || 'Nhân viên'}
                      </span>
                    </td>
                    <td className="desc-cell">{user.description || '-'}</td>
                    <td className="action-cell">
                      <button className="btn-icon btn-edit" onClick={() => handleOpenModal(user)}>
                        <Edit2 size={16} />
                      </button>
                      <button className="btn-icon btn-delete" onClick={() => handleDelete(user.id)}>
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editUser ? 'Sửa người dùng' : 'Thêm người dùng mới'}</h3>
              <button className="btn-close" onClick={handleCloseModal}>
                <X size={20} />
              </button>
            </div>
            <form className="modal-body" onSubmit={handleSubmit}>
              {error && <div className="form-error">{error}</div>}
              <div className="form-row">
                <div className="form-group">
                  <label>Họ và tên</label>
                  <input type="text" name="fullName" value={formData.fullName} onChange={handleChange} required placeholder="Nguyễn Văn A" />
                </div>
                <div className="form-group">
                  <label>Tên đăng nhập</label>
                  <input type="text" name="username" value={formData.username} onChange={handleChange} required placeholder="username" disabled={!!editUser} />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Email</label>
                  <input type="email" name="email" value={formData.email} onChange={handleChange} required placeholder="email@example.com" />
                </div>
                <div className="form-group">
                  <label>Mật khẩu {editUser && '(để trống nếu không đổi)'}</label>
                  <input type="password" name="password" value={formData.password} onChange={handleChange} required={!editUser} placeholder={editUser ? 'Không đổi' : 'Mật khẩu'} />
                </div>
              </div>
              <div className="form-group">
                <label>Chức vụ</label>
                <select name="position" value={formData.position} onChange={handleChange} required>
                  <option value="">Chọn chức vụ</option>
                  <option value="Receptionist">Lễ tân</option>
                  <option value="Manager">Quản lý</option>
                  <option value="Admin">Quản trị viên</option>
                  <option value="Staff">Nhân viên</option>
                </select>
              </div>
              <div className="form-group">
                <label>Mô tả</label>
                <textarea name="description" value={formData.description} onChange={handleChange} placeholder="Mô tả thêm..." rows="3" />
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>Hủy</button>
                <button type="submit" className="btn-primary">{editUser ? 'Lưu thay đổi' : 'Thêm mới'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
