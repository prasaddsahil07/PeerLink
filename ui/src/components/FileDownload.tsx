'use client';

import { useState } from 'react';

export default function FileDownload() {
  const [portCode, setPortCode] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async () => {
    if (!portCode.trim()) {
      setError('Please enter a valid code');
      return;
    }

    const port = parseInt(portCode.trim());
    if (isNaN(port) || port < 1000 || port > 65535) {
      setError('Please enter a valid port number (1000-65535)');
      return;
    }

    setIsDownloading(true);
    setError(null);

    try {
      const response = await fetch(`http://localhost:8080/download/${port}`, {
        method: 'GET',
      });

      if (!response.ok) {
        if (response.status === 404) {
          throw new Error('File not found or code has expired');
        } else if (response.status === 500) {
          throw new Error('Server error. Please try again.');
        } else {
          throw new Error(`Download failed: ${response.statusText}`);
        }
      }

      // Get filename from response headers
      const contentDisposition = response.headers.get('content-disposition');
      let filename = 'downloaded-file';
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      // Create blob and download
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      // Clear the input after successful download
      setPortCode('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Download failed');
    } finally {
      setIsDownloading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleDownload();
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-lg p-8">
      <h2 className="text-2xl font-bold text-gray-800 mb-6 text-center">Download File</h2>
      
      <div className="space-y-6">
        <div>
          <label htmlFor="portCode" className="block text-sm font-medium text-gray-700 mb-2">
            Enter the file code
          </label>
          <div className="relative">
            <input
              type="text"
              id="portCode"
              value={portCode}
              onChange={(e) => setPortCode(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Enter the port number (e.g., 54321)"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-center text-lg font-mono"
              disabled={isDownloading}
            />
          </div>
        </div>

        {error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-center gap-2">
              <span className="text-red-600">⚠️</span>
              <p className="text-red-700">{error}</p>
            </div>
          </div>
        )}

        <button
          onClick={handleDownload}
          disabled={!portCode.trim() || isDownloading}
          className={`w-full py-3 px-6 rounded-lg font-medium transition-all duration-200 ${
            !portCode.trim() || isDownloading
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
              : 'bg-green-600 text-white hover:bg-green-700 shadow-lg hover:shadow-xl'
          }`}
        >
          {isDownloading ? (
            <div className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              Downloading...
            </div>
          ) : (
            'Download File'
          )}
        </button>

        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-start gap-3">
            <span className="text-blue-600 text-lg">ℹ️</span>
            <div className="text-left">
              <p className="font-medium text-blue-800 mb-1">How it works:</p>
              <ul className="text-sm text-blue-700 space-y-1">
                <li>• Enter the code provided by the file sender</li>
                <li>• Files are automatically deleted after download</li>
                <li>• Codes expire after 5 minutes for security</li>
                <li>• No registration or account required</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 