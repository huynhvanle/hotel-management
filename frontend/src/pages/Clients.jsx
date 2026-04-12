import { useState, useEffect } from 'react';
import { Plus, Search, Edit2, Trash2, X, UserCircle } from 'lucide-react';
import { clientService } from '../services/api';
import './Clients.css';

export default function Clients() {
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editClient, setEditClient] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [formData, setFormData] = useState({
    idCardNumber: '',
    fullName: '',
    address: '',
    email: '',
    phone: '',
    note: '',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchClients();
  }, []);

  const fetchClients = async () => {
    try {
      setLoading(true);
      const response = await clientService.getAll();
      setClients(response.data?.clients || response.data || []);
    } catch (err) {
      console.error('Error fetching clients:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenModal = (client = null) => {
    if (client) {
      setEditClient(client);
      setFormData({
        idCardNumber: client.idCardNumber || '',
        fullName: client.fullName || '',
        address: client.address || '',
        email: client.email || '',
        phone: client.phone || '',
        note: client.note || '',
      });
    } else {
      setEditClient(null);
      setFormData({ idCardNumber: '', fullName: '', address: '', email: '', phone: '', note: '' });
    }
    setError('');
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditClient(null);
    setError('');
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editClient) {
        await clientService.update(editClient.id, formData);
      } else {
        await clientService.create(formData);
      }
      handleCloseModal();
      fetchClients();
    } catch (err) {
      setError(err.response?.data?.message || 'Có lỗi xảy ra');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa khách hàng này?')) {
      try {
        await clientService.delete(id);
        fetchClients();
      } catch (err) {
        console.error('Error deleting client:', err);
      }
    }
  };

  const filteredClients = clients.filter(
    (c) =>
      (c.fullName?.toLowerCase() || '').includes(searchTerm.toLowerCase()) ||
      (c.email?.toLowerCase() || '').includes(searchTerm.toLowerCase()) ||
      (c.phone || '').includes(searchTerm) ||
      (c.idCardNumber || '').includes(searchTerm)
  );

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>Quản lý Khách hàng</h2>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          <Plus size={18} />
          Thêm khách hàng
        </button>
      </div>

      <div className="search-bar">
        <Search size={18} />
        <input
          type="text"
          placeholder="Tìm kiếm theo tên, email, SĐT, CCCD..."
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
                <th>CCCD/CMND</th>
                <th>Email</th>
                <th>Số điện thoại</th>
                <th>Địa chỉ</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {filteredClients.length === 0 ? (
                <tr>
                  <td colSpan="7" className="empty-row">Không có dữ liệu</td>
                </tr>
              ) : (
                filteredClients.map((client, index) => (
                  <tr key={client.id}>
                    <td>{index + 1}</td>
                    <td className="name-cell">
                      <UserCircle size={18} className="client-icon" />
                      {client.fullName || '-'}
                    </td>
                    <td>{client.idCardNumber || '-'}</td>
                    <td>{client.email || '-'}</td>
                    <td>{client.phone || '-'}</td>
                    <td className="desc-cell">{client.address || '-'}</td>
                    <td className="action-cell">
                      <button className="btn-icon btn-edit" onClick={() => handleOpenModal(client)}>
                        <Edit2 size={16} />
                      </button>
                      <button className="btn-icon btn-delete" onClick={() => handleDelete(client.id)}>
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
              <h3>{editClient ? 'Sửa khách hàng' : 'Thêm khách hàng mới'}</h3>
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
                  <label>CCCD/CMND</label>
                  <input type="text" name="idCardNumber" value={formData.idCardNumber} onChange={handleChange} required placeholder="012345678901" />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Email</label>
                  <input type="email" name="email" value={formData.email} onChange={handleChange} required placeholder="email@example.com" />
                </div>
                <div className="form-group">
                  <label>Số điện thoại</label>
                  <input type="text" name="phone" value={formData.phone} onChange={handleChange} required placeholder="0912 345 678" />
                </div>
              </div>
              <div className="form-group">
                <label>Địa chỉ</label>
                <input type="text" name="address" value={formData.address} onChange={handleChange} placeholder="123 Đường ABC, Quận 1, TP.HCM" />
              </div>
              <div className="form-group">
                <label>Ghi chú</label>
                <textarea name="note" value={formData.note} onChange={handleChange} placeholder="Ghi chú thêm..." rows="3" />
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>Hủy</button>
                <button type="submit" className="btn-primary">{editClient ? 'Lưu thay đổi' : 'Thêm mới'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
