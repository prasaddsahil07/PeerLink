package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {

    private HashMap<Integer, String> availableFiles;
    private final HashMap<String, Long> fileUploadTimes = new HashMap<>();

    private static final long FILE_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final int CHUNK_SIZE = 64 * 1024; // 64KB chunks for better memory management

    public FileSharer() {
        availableFiles = new HashMap<>();
    }

    public int offerFile(String filePath) {
        int port;
        while (true) {
            port = UploadUtils.generateCode();
            if (!availableFiles.containsKey(port)) {
                availableFiles.put(port, filePath);
                try {
                    String canonicalPath = new File(filePath).getCanonicalPath();
                    fileUploadTimes.put(canonicalPath, System.currentTimeMillis());
                } catch (IOException e) {
                    System.err.println("Error getting canonical path: " + e.getMessage());
                }
                return port;
            }
        }
    }

    public void startFileServer(int port) {
        String filePath = availableFiles.get(port);
        if (filePath == null) {
            System.out.println("No file is associated with this port: " + port);
            return;
        }

        long startTime = System.currentTimeMillis();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(2000); // periodically check expiry
            System.out.println("Serving File " + new File(filePath).getName());

            while (true) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > FILE_TTL_MS) {
                    System.out.println("Code expired for port: " + port);
                    try {
                        File file = new File(filePath);
                        if (file.exists() && file.delete()) {
                            System.out.println("Expired file deleted: " + file.getName());
                        }
                    } catch (Exception ignore) {
                    }
                    removeFilePath(filePath);
                    removeUploadTimestamp(filePath);
                    break;
                }
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client Connection: " + clientSocket.getInetAddress());
                    new Thread(new FileSenderHandler(clientSocket, filePath, this, false)).start();
                } catch (SocketTimeoutException ste) {
                    // loop to re-check TTL
                }
            }
        } catch (Exception ex) {
            System.out.println("Error handling file server on port: " + port);
        }
    }

    public void removeFilePath(String path) {
        try {
            String canonicalPath = new File(path).getCanonicalPath();
            availableFiles.entrySet().removeIf(entry -> {
                try {
                    return new File(entry.getValue()).getCanonicalPath().equals(canonicalPath);
                } catch (IOException e) {
                    return false;
                }
            });
        } catch (IOException e) {
            System.err.println("Error resolving canonical path for removal: " + e.getMessage());
        }
    }

    public Long getUploadTime(String path) {
        try {
            return fileUploadTimes.get(new File(path).getCanonicalPath());
        } catch (IOException e) {
            return null;
        }
    }

    public void removeUploadTimestamp(String path) {
        try {
            fileUploadTimes.remove(new File(path).getCanonicalPath());
        } catch (IOException e) {
        }
    }

    private static class FileSenderHandler implements Runnable {

        private final Socket clientSocket;
        private final String filePath;
        private final FileSharer fileSharer;
        private final boolean deleteAfterSend;

        public FileSenderHandler(Socket clientSocket, String filePath, FileSharer fileSharer, boolean deleteAfterSend) {
            this.clientSocket = clientSocket;
            this.filePath = filePath;
            this.fileSharer = fileSharer;
            this.deleteAfterSend = deleteAfterSend;
        }

        @Override
        public void run() {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    System.err.println("File no longer exists: " + filePath);
                    return;
                }

                try (FileInputStream fis = new FileInputStream(filePath)) {
                    OutputStream oos = clientSocket.getOutputStream();

                    String fileName = file.getName();
                    long fileSize = file.length();

                    // Send header with file info
                    // String header = String.format("Filename: %s\nSize: %d\n", fileName, fileSize);
                    String header = "Filename: " + filename + "\n";
                    oos.write(header.getBytes());

                    // Send file in chunks with progress tracking
                    byte[] buffer = new byte[CHUNK_SIZE];
                    long totalBytesSent = 0;
                    int chunkCount = 0;

                    System.out.println("Starting chunked transfer of " + fileName + " (" + fileSize + " bytes)");

                    while (true) {
                        int bytesRead = fis.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }

                        oos.write(buffer, 0, bytesRead);
                        oos.flush(); // Ensure chunks are sent immediately

                        totalBytesSent += bytesRead;
                        chunkCount++;

                        // Log progress every 10 chunks or when complete
                        if (chunkCount % 10 == 0 || totalBytesSent == fileSize) {
                            double progress = (double) totalBytesSent / fileSize * 100;
                            System.out.printf("File %s: %d/%d bytes sent (%.1f%%) - %d chunks%n",
                                    fileName, totalBytesSent, fileSize, progress, chunkCount);
                        }
                    }

                    System.out.println("File " + fileName + " sent to " + clientSocket.getInetAddress()
                            + " in " + chunkCount + " chunks");

                    if (deleteAfterSend) {
                        if (file.delete()) {
                            System.out.println("File deleted successfully: " + fileName);
                            fileSharer.removeFilePath(filePath);
                            fileSharer.removeUploadTimestamp(filePath);
                        } else {
                            System.err.println("Failed to delete file: " + fileName);
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error sending file to the client " + ex.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception ex) {
                    System.err.println("Error closing socket: " + ex.getMessage());
                }
            }
        }

    }
}
