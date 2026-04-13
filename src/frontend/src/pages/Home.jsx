import Navbar from "../components/Navbar";
import Hero from "../components/Hero";
import RoomCard from "../components/RoomCard";

export default function Home() {
  const rooms = [
    { name: "Standard Room", price: "$50/night" },
    { name: "Deluxe Room", price: "$90/night" },
    { name: "VIP Suite", price: "$150/night" },
  ];

  return (
    <div className="bg-gray-100 min-h-screen">
      <Navbar />
      <Hero />

      <div className="px-10 py-16">
        <h2 className="text-3xl font-bold text-center mb-10">
          Popular Rooms
        </h2>

        <div className="grid md:grid-cols-3 gap-8">
          {rooms.map((room, index) => (
            <RoomCard
              key={index}
              name={room.name}
              price={room.price}
            />
          ))}
        </div>
      </div>
    </div>
  );
}