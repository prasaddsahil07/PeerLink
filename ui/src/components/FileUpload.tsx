'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';

const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB limit

export default function FileUpload() {
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadedPort, setUploadedPort] = useState<number | null>(null);
  const [isCopied, setIsCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const onDrop = useCallback((acceptedFiles: File[], rejectedFiles: any[]) => {
    if (rejectedFiles.length > 0) {
      const rejection = rejectedFiles[0];
      if (rejection.errors[0]?.code === 'file-too-large') {
        setError('File is too large. Maximum size is 100MB.');
      } else if (rejection.errors[0]?.code === 'file-invalid-type') {
        setError('File type not supported. Please select a valid file.');
      } else {
        setError('File upload failed. Please try again.');
      }
      return;
    }

    if (acceptedFiles.length > 0) {
      const selectedFile = acceptedFiles[0];
      if (selectedFile.size > MAX_FILE_SIZE) {
        setError('File is too large. Maximum size is 100MB.');
        return;
      }
      setFile(selectedFile);
      setError(null);
      setUploadedPort(null);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'image/*': ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp', '.tiff'],
      'application/pdf': ['.pdf'],
      'audio/*': ['.mp3', '.wav', '.ogg', '.m4a', '.aac'],
      'video/*': ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm', '.mkv'],
      'text/*': ['.txt', '.md', '.json', '.xml', '.csv'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'application/vnd.ms-excel': ['.xls'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'application/vnd.ms-powerpoint': ['.ppt'],
      'application/vnd.openxmlformats-officedocument.presentationml.presentation': ['.pptx'],
      'application/zip': ['.zip'],
      'application/x-rar-compressed': ['.rar'],
    },
    maxSize: MAX_FILE_SIZE,
    multiple: false,
  });

  const handleUpload = async () => {
    if (!file) return;

    setIsUploading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/upload`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        if (response.status === 413) {
          throw new Error('File too large. Maximum size is 100MB.');
        } else {
          throw new Error('Upload failed. Please try again.');
        }
      }

      const result = await response.json();
      setUploadedPort(result.port);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const copyToClipboard = async () => {
    if (uploadedPort) {
      try {
        await navigator.clipboard.writeText(uploadedPort.toString());
        setIsCopied(true);
        setTimeout(() => setIsCopied(false), 5000); // Reset after 5 seconds
      } catch (err) {
        console.error('Failed to copy to clipboard:', err);
      }
    }
  };

  return (
    <div className="bg-gray-800 rounded-2xl shadow-2xl p-8 hover-lift">
      <h2 className="text-2xl font-bold text-white mb-6 text-center">Upload File</h2>
      
      <div className="space-y-6">
        <div
          {...getRootProps()}
          className={`max-w-md mx-auto border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-300 ${
            isDragActive
              ? 'border-blue-500 bg-blue-900/20'
              : 'border-gray-600 hover:border-gray-500 bg-gray-700/50'
          }`}
        >
          <input {...getInputProps()} />
          <div className="space-y-4">
            <div className="text-4xl">📁</div>
            {isDragActive ? (
              <p className="text-blue-400 font-medium">Drop the file here...</p>
            ) : (
              <div>
                <p className="text-gray-300 font-medium mb-2">
                  Drag & drop a file here, or click to select
                </p>
                <p className="text-gray-400 text-sm">
                  Supports: Images, PDFs, Audio, Video, Documents, Archives
                </p>
                <p className="text-gray-400 text-sm mt-1">
                  Max size: 100MB
                </p>
              </div>
            )}
          </div>
        </div>

        {file && (
          <div className="bg-gray-700/50 rounded-xl p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <span className="text-2xl">
                  {file.type.startsWith('image/') ? '🖼️' :
                   file.type.startsWith('video/') ? '🎥' :
                   file.type.startsWith('audio/') ? '🎵' :
                   file.type === 'application/pdf' ? '📄' :
                   '📎'}
                </span>
                <div>
                  <p className="text-white font-medium truncate">{file.name}</p>
                  <p className="text-gray-400 text-sm">{formatFileSize(file.size)}</p>
                </div>
              </div>
              <button
                onClick={() => setFile(null)}
                className="text-gray-400 hover:text-red-400 transition-colors"
              >
                ✕
              </button>
            </div>
          </div>
        )}

        {error && (
          <div className="p-4 bg-red-900/20 border border-red-800 rounded-xl">
            <div className="flex items-center gap-2">
              <span className="text-red-400">⚠️</span>
              <p className="text-red-300">{error}</p>
            </div>
          </div>
        )}

        {uploadedPort && (
          <div className="bg-green-900/20 border border-green-800 rounded-xl p-6 text-center">
            <div className="text-4xl mb-4">🎉</div>
            <h3 className="text-green-400 font-bold text-xl mb-2">File Uploaded Successfully!</h3>
            <p className="text-gray-300 mb-4">Share this code with others to let them download your file:</p>
            <div className="bg-gray-700 rounded-lg p-4 mb-4">
              <div className="flex items-center justify-center gap-6">
                <p className="text-3xl font-mono font-bold text-green-400 tracking-wider">
                  {uploadedPort}
                </p>
                <button
                  onClick={copyToClipboard}
                  className={`bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors duration-200 font-medium ${
                    isCopied ? 'bg-gray-600 cursor-not-allowed' : ''
                  }`}
                  title="Copy code"
                  disabled={isCopied}
                >
                  {isCopied ? 'Copied!' : 'Copy'}
                </button>
              </div>
            </div>
            <div className="text-sm text-gray-400 space-y-1">
              <p>• Code expires in 5 minutes</p>
              <p>• Multiple users can download with this code</p>
              <p>• File will be automatically deleted after expiry</p>
            </div>
          </div>
        )}

        <button
          onClick={handleUpload}
          disabled={!file || isUploading}
          className={`w-full py-3 px-6 rounded-lg font-medium transition-all duration-200 ${
            !file || isUploading
              ? 'bg-gray-600 text-gray-400 cursor-not-allowed'
              : 'bg-blue-600 text-white hover:bg-blue-700 shadow-lg hover:shadow-xl'
          }`}
        >
          {isUploading ? (
            <div className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              Uploading...
            </div>
          ) : (
            'Upload File'
          )}
        </button>

        <div className="bg-blue-900/20 border border-blue-800 rounded-xl p-4">
          <div className="flex items-start gap-3">
            <span className="text-blue-400 text-lg">ℹ️</span>
            <div className="text-left">
              <p className="font-medium text-blue-300 mb-1">How it works:</p>
              <ul className="text-sm text-gray-300 space-y-1">
                <li>• Upload any file up to 100MB</li>
                <li>• Get a unique code to share</li>
                <li>• Others can download using the code</li>
                <li>• Files expire after 5 minutes</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 