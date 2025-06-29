'use client'

import { useState } from 'react'
import { Upload, Download, Share2, Link } from 'lucide-react'

export default function Home() {
  const [activeTab, setActiveTab] = useState<'upload' | 'download'>('upload')
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadResult, setUploadResult] = useState<{ success: boolean; message: string; port?: string } | null>(null)
  const [downloadPort, setDownloadPort] = useState('')
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState('')

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0]
    if (selectedFile) {
      setFile(selectedFile)
      setUploadResult(null)
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    const droppedFile = e.dataTransfer.files[0]
    if (droppedFile) {
      setFile(droppedFile)
      setUploadResult(null)
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
  }

  const handleUpload = async () => {
    if (!file) return

    setUploading(true)
    setUploadResult(null)

    try {
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch('http://localhost:8000/upload', {
        method: 'POST',
        body: formData,
      })

      const result = await response.json()

      if (response.ok) {
        setUploadResult({
          success: true,
          message: 'File uploaded successfully!',
          port: result.port
        })
        setFile(null)
      } else {
        setUploadResult({
          success: false,
          message: result.error || 'Upload failed'
        })
      }
    } catch (error) {
      setUploadResult({
        success: false,
        message: 'Network error. Please try again.'
      })
    } finally {
      setUploading(false)
    }
  }

  const handleDownload = async () => {
    if (!downloadPort.trim()) {
      setDownloadError('Please enter a port number')
      return
    }

    setDownloading(true)
    setDownloadError('')

    try {
      const response = await fetch(`http://localhost:8000/download/${downloadPort}`)
      
      if (!response.ok) {
        throw new Error('File not found or port expired')
      }

      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'downloaded-file'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      setDownloadPort('')
    } catch (error) {
      setDownloadError(error instanceof Error ? error.message : 'Download failed')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center text-white shadow-lg">
                <Share2 className="w-4 h-4" />
              </div>
              <div className="flex items-center space-x-1">
                <span className="font-bold text-xl bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                  Peer
                </span>
                <Link className="w-5 h-5 text-indigo-600" />
              </div>
            </div>
            <h1 className="text-2xl font-bold text-gray-900">PeerLink</h1>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
        {/* Tab Navigation */}
        <div className="flex justify-center mb-8">
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-1">
            <button
              onClick={() => setActiveTab('upload')}
              className={`px-6 py-2 rounded-md font-medium transition-colors duration-200 ${
                activeTab === 'upload'
                  ? 'bg-primary-600 text-white'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Upload File
            </button>
            <button
              onClick={() => setActiveTab('download')}
              className={`px-6 py-2 rounded-md font-medium transition-colors duration-200 ${
                activeTab === 'download'
                  ? 'bg-primary-600 text-white'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Download File
            </button>
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'upload' ? (
          <div className="card">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Upload File</h2>
            
            {/* File Drop Zone */}
            <div
              className={`border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-blue-400 hover:bg-blue-50 transition-colors duration-200 cursor-pointer ${
                file ? 'border-blue-400 bg-blue-50' : ''
              }`}
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onClick={() => document.getElementById('file-input')?.click()}
            >
              <input
                id="file-input"
                type="file"
                onChange={handleFileSelect}
                className="hidden"
                accept="*/*"
              />
              
              <div className="space-y-4">
                <div className="flex justify-center">
                  <div className={`w-16 h-16 rounded-lg flex items-center justify-center ${
                    file ? 'bg-blue-100' : 'bg-gray-100'
                  }`}>
                    {file ? (
                      <Upload className="w-8 h-8 text-blue-600" />
                    ) : (
                      <Upload className="w-8 h-8 text-gray-400" />
                    )}
                  </div>
                </div>
                
                <div>
                  <h3 className="text-lg font-semibold text-gray-800 mb-2">
                    {file ? 'File Selected' : 'Drop your file here'}
                  </h3>
                  <p className="text-gray-600">
                    {file 
                      ? `${file.name} (${(file.size / 1024 / 1024).toFixed(2)} MB)`
                      : 'or click to browse files'
                    }
                  </p>
                </div>
              </div>
            </div>

            {/* Upload Button */}
            {file && (
              <button
                onClick={handleUpload}
                disabled={uploading}
                className="btn-primary w-full mt-6 flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {uploading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Uploading...</span>
                  </>
                ) : (
                  <>
                    <Upload className="w-5 h-5" />
                    <span>Upload File</span>
                  </>
                )}
              </button>
            )}

            {/* Upload Result */}
            {uploadResult && (
              <div className={`mt-6 p-4 rounded-lg ${
                uploadResult.success 
                  ? 'bg-green-50 border border-green-200' 
                  : 'bg-red-50 border border-red-200'
              }`}>
                <div className="flex items-start space-x-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
                    uploadResult.success ? 'bg-green-100' : 'bg-red-100'
                  }`}>
                    {uploadResult.success ? (
                      <span className="text-green-600 text-sm">✓</span>
                    ) : (
                      <span className="text-red-600 text-sm">✕</span>
                    )}
                  </div>
                  <div className="flex-1">
                    <h3 className={`font-semibold mb-2 ${
                      uploadResult.success ? 'text-green-800' : 'text-red-800'
                    }`}>
                      {uploadResult.success ? 'Upload Successful!' : 'Upload Failed'}
                    </h3>
                    <p className={`text-sm ${
                      uploadResult.success ? 'text-green-700' : 'text-red-700'
                    }`}>
                      {uploadResult.message}
                    </p>
                    
                    {uploadResult.success && uploadResult.port && (
                      <div className="mt-4 p-4 bg-white rounded-lg border border-green-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-gray-700">Download Port:</span>
                          <button
                            onClick={() => navigator.clipboard.writeText(uploadResult.port!)}
                            className="p-1 hover:bg-gray-100 rounded transition-colors duration-200"
                          >
                            Copy
                          </button>
                        </div>
                        <p className="text-2xl font-bold text-gray-800 font-mono">
                          {uploadResult.port}
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          Valid for 5 minutes
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="card">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Download File</h2>
            
            <div className="space-y-4">
              <div>
                <label htmlFor="port-input" className="block text-sm font-medium text-gray-700 mb-2">
                  Enter Port Number
                </label>
                <input
                  type="number"
                  id="port-input"
                  value={downloadPort}
                  onChange={(e) => setDownloadPort(e.target.value)}
                  placeholder="e.g., 54321"
                  className="input-field"
                />
              </div>

              <button
                onClick={handleDownload}
                disabled={!downloadPort.trim() || downloading}
                className="btn-primary w-full flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {downloading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Downloading...</span>
                  </>
                ) : (
                  <>
                    <Download className="w-5 h-5" />
                    <span>Download File</span>
                  </>
                )}
              </button>

              {downloadError && (
                <div className="mt-4 bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center space-x-2">
                    <span className="text-red-600">✕</span>
                    <p className="text-red-800">{downloadError}</p>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  )
} 