import { Building2, Star, MapPin } from 'lucide-react';
import './Hotels.css';

export default function Hotels() {
  return (
    <div className="page-container">
      <div className="page-header">
        <h2>Quản lý Khách sạn</h2>
      </div>
      <div className="coming-soon">
        <div className="coming-soon-icon">
          <Building2 size={48} />
        </div>
        <h3>Tính năng đang phát triển</h3>
        <p>Trang quản lý khách sạn đang được xây dựng. Vui lòng quay lại sau.</p>
      </div>
    </div>
  );
}
