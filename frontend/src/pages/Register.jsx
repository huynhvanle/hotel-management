import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff, UserPlus } from 'lucide-react';
import './Auth.css';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    fullName: '',
    email: '',
    position: '',
    description: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(formData);
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-header">
          <h1>HotelPro</h1>
          <p>Đăng ký tài khoản mới</p>
        </div>
        <form className="auth-form" onSubmit={handleSubmit}>
          {error && <div className="auth-error">{error}</div>}
          <div className="form-row">
            <div className="form-group">
              <label>Tên đăng nhập</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                placeholder="Tên đăng nhập"
                required
              />
            </div>
            <div className="form-group">
              <label>Mật khẩu</label>
              <div className="password-input">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Mật khẩu"
                  required
                />
                <button type="button" className="toggle-password" onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Họ và tên</label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                placeholder="Nguyễn Văn A"
                required
              />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="email@example.com"
                required
              />
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
            <label>Mô tả (tùy chọn)</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Mô tả thêm về bạn..."
              rows="3"
            />
          </div>
          <button type="submit" className="auth-btn" disabled={loading}>
            <UserPlus size={18} />
            {loading ? 'Đang đăng ký...' : 'Đăng ký'}
          </button>
        </form>
        <div className="auth-footer">
          <p>
            Đã có tài khoản? <Link to="/login">Đăng nhập ngay</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
