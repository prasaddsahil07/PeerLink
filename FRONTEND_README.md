# Frontend Architecture - AnyWhere Door

## 🏗 Component Architecture

### Core Components
1. **Header.tsx** - Animated header with project branding
2. **FileUpload.tsx** - Drag & drop file upload with progress
3. **FileDownload.tsx** - Code-based file download
4. **page.tsx** - Main page with tab navigation

## 🎨 UI/UX Design

### Design System
- **Theme**: Dark mode only
- **Colors**: Gray gradients (900-800-900)
- **Typography**: Modern sans-serif fonts
- **Animations**: Tailwind CSS animations

### Key Features
- **Drag & Drop**: React Dropzone integration
- **Copy to Clipboard**: Native clipboard API
- **Success Feedback**: Dynamic state changes
- **Responsive Design**: Mobile-first approach

## 🔧 Technical Implementation

### State Management
```typescript
// File Upload State
const [file, setFile] = useState<File | null>(null);
const [isUploading, setIsUploading] = useState(false);
const [uploadedPort, setUploadedPort] = useState<number | null>(null);
const [isCopied, setIsCopied] = useState(false);

// File Download State
const [downloadCode, setDownloadCode] = useState('');
const [isDownloading, setIsDownloading] = useState(false);
const [downloadSuccess, setDownloadSuccess] = useState(false);
```

### File Upload Flow
1. **Drag & Drop**: React Dropzone handles file selection
2. **Validation**: File size and type checking
3. **Upload**: Multipart form data to backend
4. **Response**: Port code generation
5. **Feedback**: Success message with copy functionality

### File Download Flow
1. **Code Input**: User enters sharing code
2. **Validation**: Backend port verification
3. **Socket Connection**: Direct P2P file transfer
4. **Progress**: Real-time download tracking
5. **Completion**: Success message display

## 🎯 Key Features

### Drag & Drop Upload
```typescript
const { getRootProps, getInputProps, isDragActive } = useDropzone({
  onDrop: onDrop,
  accept: {
    'image/*': ['.png', '.jpg', '.jpeg', '.gif'],
    'application/pdf': ['.pdf'],
    'text/*': ['.txt', '.md'],
    'audio/*': ['.mp3', '.wav', '.ogg'],
    'video/*': ['.mp4', '.avi', '.mov']
  }
});
```

### Copy to Clipboard
```typescript
const copyToClipboard = async () => {
  if (uploadedPort) {
    try {
      await navigator.clipboard.writeText(uploadedPort.toString());
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  }
};
```

### File Type Icons
```typescript
const getFileIcon = (fileType: string) => {
  if (fileType.startsWith('image/')) return '🖼️';
  if (fileType === 'application/pdf') return '📄';
  if (fileType.startsWith('text/')) return '📝';
  if (fileType.startsWith('audio/')) return '🎵';
  if (fileType.startsWith('video/')) return '🎬';
  return '📁';
};
```

## 🎨 Styling Strategy

### Tailwind CSS Classes
- **Gradients**: `bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900`
- **Glass Effect**: `backdrop-blur-sm bg-white/10 border border-white/20`
- **Animations**: `animate-bounce`, `animate-pulse`, `hover-lift`
- **Responsive**: `max-w-md mx-auto`, `p-6 md:p-8`

### Custom Animations
```css
@keyframes animate-blob {
  0%, 100% { transform: translate(0px, 0px) scale(1); }
  33% { transform: translate(30px, -50px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}
```

## 🔍 Interview Talking Points

### Performance Optimization
- **Code Splitting**: Next.js automatic code splitting
- **Image Optimization**: Next.js Image component
- **Bundle Analysis**: Webpack bundle analyzer
- **Lazy Loading**: Component lazy loading

### Accessibility
- **ARIA Labels**: Screen reader support
- **Keyboard Navigation**: Tab and Enter key support
- **Color Contrast**: WCAG compliant color ratios
- **Focus Management**: Proper focus indicators

### Error Handling
- **Network Errors**: Fetch API error handling
- **File Validation**: Client-side file checks
- **User Feedback**: Clear error messages
- **Fallback States**: Loading and error states

## 🚀 Deployment

### Development
```bash
cd ui
npm install
npm run dev
```

### Production Build
```bash
npm run build
npm start
```

### Docker
```bash
docker build -f Dockerfile.frontend -t anywhere-door-frontend .
docker run -p 3000:3000 anywhere-door-frontend
```

## 🔧 Configuration

### Environment Variables
- **API URL**: `NEXT_PUBLIC_API_URL=http://localhost:8080`
- **Build Mode**: Production/Development
- **Port**: 3000 (default)

### Key Dependencies
- **Next.js**: 14.x (App Router)
- **React**: 18.x
- **TypeScript**: 5.x
- **Tailwind CSS**: 3.x
- **React Dropzone**: 14.x

## 📱 Responsive Design

### Breakpoints
- **Mobile**: < 768px
- **Tablet**: 768px - 1024px
- **Desktop**: > 1024px

### Mobile Optimizations
- Touch-friendly buttons
- Swipe gestures
- Optimized file picker
- Responsive typography
