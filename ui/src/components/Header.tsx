export default function Header() {
  return (
    <div className="text-center mb-12">
      <div className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full mb-4 shadow-lg">
        <span className="text-3xl">🚪</span>
      </div>
      <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent mb-2">
        AnyWhere Door
      </h1>
      <p className="text-gray-600 text-lg max-w-md mx-auto">
        Share files instantly with a unique code. Your files disappear after download for maximum privacy.
      </p>
    </div>
  );
} 