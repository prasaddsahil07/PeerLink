# AnyWhere Door - Peer-to-Peer File Sharing

A modern peer-to-peer file sharing application built with Java backend and Next.js frontend.

## 🚀 Quick Start

### Using Docker (Recommended)
```bash
# Build and start all services
docker-compose up --build

# Access the application
Frontend: http://localhost:3000
Backend: http://localhost:8080
```

### Manual Setup
```bash
# Backend
cd src/main/java/p2p
mvn clean package
java -jar target/p2p-1.0-SNAPSHOT.jar

# Frontend
cd ui
npm install
npm run dev
```

## 📁 Project Structure
```
PeerLink/
├── src/main/java/p2p/          # Java Backend
│   ├── App.java                # Main entry point
│   ├── controller/             # HTTP endpoints
│   ├── service/                # Core business logic
│   └── utils/                  # Utility classes
├── ui/                         # Next.js Frontend
│   ├── src/app/               # App router pages
│   └── src/components/        # React components
├── Dockerfile.backend         # Backend container
├── Dockerfile.frontend        # Frontend container
└── docker-compose.yml         # Multi-service orchestration
```

## 🔧 Key Features
- **File Upload**: Drag & drop any file type
- **Code Generation**: Random port-based sharing codes
- **Multiple Downloads**: Unlimited downloads within 5-minute window
- **Auto Cleanup**: Files expire after 5 minutes
- **Modern UI**: Dark theme with animations
- **Real-time Progress**: Chunked file transfer

## 🛠 Tech Stack
- **Backend**: Java 17, HTTP Server, Socket Programming
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS
- **Containerization**: Docker, Docker Compose
- **File Transfer**: Chunked streaming (64KB chunks)
