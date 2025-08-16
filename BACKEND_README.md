# Backend Architecture - AnyWhere Door

## 🏗 System Architecture

### Core Components
1. **App.java** - Main entry point, starts HTTP server
2. **FileController** - HTTP endpoints and request handling
3. **FileSharer** - Core P2P file sharing logic
4. **UploadUtils** - Port generation utilities

## 🔧 Key Technical Decisions

### 1. Multi-threading Strategy
```java
// HTTP Server Thread Pool
server.setExecutor(Executors.newFixedThreadPool(40));

// File Server Threads
new Thread(() -> fileSharer.startFileServer(port, file)).start();

// Client Connection Threads
new Thread(new FileSenderHandler(socket, file, false)).start();
```

### 2. File Transfer Protocol
- **Chunked Transfer**: 64KB chunks for large files
- **Header Protocol**: `Filename: name\nSize: bytes\n`
- **Progress Tracking**: Real-time chunk counting

### 3. Port Generation
```java
// Dynamic port range (49152-65535)
return DYNAMIC_STARTING_PORT + random.nextInt(RANGE);
```

## 📡 API Endpoints

### POST /upload
- **Purpose**: File upload and code generation
- **Input**: Multipart form data
- **Output**: `{"port": 12345}`
- **Features**: 
  - File size validation (100MB limit)
  - CORS handling
  - Random port assignment

### POST /download
- **Purpose**: Get file server details
- **Input**: `{"port": 12345}`
- **Output**: Socket connection details
- **Features**: Port validation, file existence check

## 🔄 File Lifecycle

1. **Upload**: File stored in temp directory
2. **Sharing**: Random port assigned, file server started
3. **Download**: Multiple clients can download simultaneously
4. **Expiry**: 5-minute TTL, automatic cleanup

## 🛡 Security & Performance

### CORS Configuration
```java
headers.add("Access-Control-Allow-Origin", "*");
headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With");
```

### Memory Management
- **File Size Limit**: 100MB per file
- **Thread Pool**: 40 concurrent HTTP requests
- **Cleanup**: Scheduled every minute

## 🔍 Interview Talking Points

### Scalability
- **Horizontal**: Multiple server instances
- **Vertical**: Increased thread pool size
- **Load Balancing**: Multiple file servers per port

### Error Handling
- **File Not Found**: 404 responses
- **Invalid Port**: Port validation
- **File Too Large**: Size limit enforcement

### Monitoring
- **Console Logging**: Real-time operation tracking
- **File Cleanup**: Automatic expired file removal
- **Connection Tracking**: Client connection monitoring

## 🚀 Deployment

### Docker
```bash
# Build
docker build -f Dockerfile.backend -t anywhere-door-backend .

# Run
docker run -p 8080:8080 anywhere-door-backend
```

### Manual
```bash
mvn clean package
java -jar target/p2p-1.0-SNAPSHOT.jar
```

## 🔧 Configuration

### Key Constants
- **HTTP Port**: 8080
- **File TTL**: 5 minutes
- **Chunk Size**: 64KB
- **Max File Size**: 100MB
- **Thread Pool**: 40 threads
