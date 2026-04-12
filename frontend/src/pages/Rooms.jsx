import { BedDouble } from 'lucide-react';
import './Rooms.css';

export default function Rooms() {
  return (
    <div className="page-container">
      <div className="page-header">
        <h2>Quản lý Phòng</h2>
      </div>
      <div className="coming-soon">
        <div className="coming-soon-icon">
          <BedDouble size={48} />
        </div>
        <h3>Tính năng đang phát triển</h3>
        <p>Trang quản lý phòng đang được xây dựng. Vui lòng quay lại sau.</p>
      </div>
    </div>
  );
}
