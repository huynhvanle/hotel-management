export default function Sidebar() {
  return (
    <div className="w-64 bg-slate-900 text-white min-h-screen p-5">
      <h1 className="text-2xl font-bold mb-8">Hotel Admin</h1>

      <ul className="space-y-3">
        <li className="p-3 rounded-lg hover:bg-slate-700 cursor-pointer">
          Dashboard
        </li>

        <li className="p-3 rounded-lg hover:bg-slate-700 cursor-pointer">
          Rooms
        </li>

        <li className="p-3 rounded-lg hover:bg-slate-700 cursor-pointer">
          Booking
        </li>

        <li className="p-3 rounded-lg hover:bg-slate-700 cursor-pointer">
          Clients
        </li>

        <li className="p-3 rounded-lg hover:bg-slate-700 cursor-pointer">
          Billing
        </li>

        <li className="p-3 rounded-lg hover:bg-slate-700 cursor-pointer">
          Stats
        </li>
      </ul>
    </div>
  );
}