import { CalendarCheck } from 'lucide-react';
import './Bookings.css';

export default function Bookings() {
  return (
    <div className="page-container">
      <div className="page-header">
        <h2>Quản lý Đặt phòng</h2>
      </div>
      <div className="coming-soon">
        <div className="coming-soon-icon">
          <CalendarCheck size={48} />
        </div>
        <h3>Tính năng đang phát triển</h3>
        <p>Trang quản lý đặt phòng đang được xây dựng. Vui lòng quay lại sau.</p>
      </div>
    </div>
  );
}
