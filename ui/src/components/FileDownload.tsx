'use client';

import { useState } from 'react';

export default function FileDownload() {
  const [portCode, setPortCode] = useState('');
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);
  const [downloadedFileName, setDownloadedFileName] = useState('');
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
    setDownloadSuccess(false);

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/download/${port}`, {
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

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);

      // const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;

      const headers = response.headers;
      let contentDisposition = '';


      for(const key in headers){
        if(key.toLowerCase() === 'content-disposition'){
          contentDisposition = headers[key];
          break;
        }
      }

      // Get filename from response headers
      // const contentDisposition = response.headers.get('content-disposition');
      let filename = 'downloaded-file';
      
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch && filenameMatch.length === 2) {
          filename = filenameMatch[1];
        }
      }

      // Create blob and download

      // const a = document.createElement('a');
      // a.href = url;
      // a.download = filename;
      // a.click();
      // window.URL.revokeObjectURL(url);
      // document.body.removeChild(a);

      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();

      // Set success state
      setDownloadSuccess(true);
      setDownloadedFileName(filename);
      
      // Clear the input after successful download
      setPortCode('');
      
      // Reset success state after 5 seconds
      setTimeout(() => {
        setDownloadSuccess(false);
        setDownloadedFileName('');
      }, 5000);
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
    <div className="bg-gray-800 rounded-2xl shadow-2xl p-8 hover-lift">
      <h2 className="text-2xl font-bold text-white mb-6 text-center">Download File</h2>
      
      <div className="space-y-6">
        <div>
          <label htmlFor="portCode" className="block text-sm font-medium text-gray-300 mb-2">
            Enter the file code
          </label>
          <div className="relative">
            <input
              type="text"
              id="portCode"
              value={portCode}
              onChange={(e) => setPortCode(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Enter the code number (e.g., 54321)"
              className="w-full px-6 py-4 border-2 border-gray-600 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent text-center text-xl font-mono bg-gray-700 text-white placeholder-gray-400 transition-all duration-300"
              disabled={isDownloading}
            />
          </div>
        </div>

        {error && (
          <div className="p-4 bg-red-900/20 border border-red-800 rounded-xl">
            <div className="flex items-center gap-2">
              <span className="text-red-400">⚠️</span>
              <p className="text-red-300">{error}</p>
            </div>
          </div>
        )}

        {downloadSuccess && (
          <div className="bg-green-900/20 border border-green-800 rounded-xl p-6 text-center">
            <div className="text-4xl mb-4">✅</div>
            <h3 className="text-green-400 font-bold text-xl mb-2">Download Completed!</h3>
            <p className="text-gray-300 mb-4">File has been successfully downloaded:</p>
            <div className="bg-gray-700 rounded-lg p-4 mb-4">
              <p className="text-lg font-mono text-green-400">
                {downloadedFileName}
              </p>
            </div>
            <div className="text-sm text-gray-400 space-y-1">
              <p>• Check your downloads folder</p>
              <p>• File is ready to use</p>
            </div>
          </div>
        )}

        <button
          onClick={handleDownload}
          disabled={isDownloading || !portCode.trim()}
          className={`w-full py-3 px-6 rounded-lg font-medium transition-all duration-200 ${
            !portCode.trim() || isDownloading
              ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
              : 'bg-blue-600 text-white hover:bg-blue-700 shadow-lg hover:shadow-xl'
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

        <div className="bg-blue-900/20 border border-blue-800 rounded-xl p-4">
          <div className="flex items-start gap-3">
            <span className="text-blue-400 text-lg">ℹ️</span>
            <div className="text-left">
              <p className="font-medium text-blue-300 mb-1">How it works:</p>
              <ul className="text-sm text-gray-300 space-y-1">
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
