// src/components/RoomCard.jsx
export default function RoomCard({ name, price }) {
  return (
    <div className="bg-white rounded-2xl shadow-md p-6 hover:shadow-xl transition">
      <div className="h-40 bg-gray-200 rounded-xl mb-5"></div>

      <h3 className="text-xl font-semibold mb-2">
        {name}
      </h3>

      <p className="text-blue-600 font-bold mb-4">
        {price}
      </p>

      <button className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700">
        View Details
      </button>
    </div>
  );
}