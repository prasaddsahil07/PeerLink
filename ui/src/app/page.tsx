'use client';

import { useState } from 'react';
import FileUpload from '@/components/FileUpload';
import FileDownload from '@/components/FileDownload';
import Header from '@/components/Header';

export default function Home() {
  const [activeTab, setActiveTab] = useState<'upload' | 'download'>('upload');

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      <div className="container mx-auto px-4 py-8">
        <Header />
        
        {/* Tab Navigation */}
        <div className="flex justify-center mb-8">
          <div className="bg-white rounded-lg p-1 shadow-lg">
            <button
              onClick={() => setActiveTab('upload')}
              className={`px-6 py-3 rounded-md font-medium transition-all duration-200 ${
                activeTab === 'upload'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-gray-600 hover:text-blue-600 hover:bg-blue-50'
              }`}
            >
              📤 Upload File
            </button>
            <button
              onClick={() => setActiveTab('download')}
              className={`px-6 py-3 rounded-md font-medium transition-all duration-200 ${
                activeTab === 'download'
                  ? 'bg-green-600 text-white shadow-md'
                  : 'text-gray-600 hover:text-green-600 hover:bg-green-50'
              }`}
            >
              📥 Download File
            </button>
          </div>
        </div>

        {/* Main Content */}
        <div className="max-w-2xl mx-auto">
          {activeTab === 'upload' ? <FileUpload /> : <FileDownload />}
        </div>
      </div>
    </div>
  );
}
