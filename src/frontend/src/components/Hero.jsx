// src/components/Hero.jsx
export default function Hero() {
  return (
    <section className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white text-center py-24">
      <h1 className="text-5xl font-bold mb-6">
        Welcome to Luxury Hotel
      </h1>

      <p className="text-lg mb-8">
        Enjoy comfort, elegance and unforgettable experiences.
      </p>

      <button className="bg-white text-blue-700 px-6 py-3 rounded-xl font-semibold hover:bg-gray-100">
        Book Now
      </button>
    </section>
  );
}