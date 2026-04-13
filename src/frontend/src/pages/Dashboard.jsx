export default function Dashboard() {
  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Hotel Overview</h1>

      <div className="grid grid-cols-4 gap-5">
        <div className="bg-white p-5 rounded-xl shadow">
          <p className="text-gray-500">Total Rooms</p>
          <h2 className="text-3xl font-bold">50</h2>
        </div>

        <div className="bg-white p-5 rounded-xl shadow">
          <p className="text-gray-500">Available</p>
          <h2 className="text-3xl font-bold text-green-600">20</h2>
        </div>

        <div className="bg-white p-5 rounded-xl shadow">
          <p className="text-gray-500">Booked</p>
          <h2 className="text-3xl font-bold text-red-500">30</h2>
        </div>

        <div className="bg-white p-5 rounded-xl shadow">
          <p className="text-gray-500">Revenue</p>
          <h2 className="text-3xl font-bold text-blue-600">15M</h2>
        </div>
      </div>
    </div>
  );
}