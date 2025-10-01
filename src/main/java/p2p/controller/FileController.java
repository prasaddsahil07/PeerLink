package p2p.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import p2p.service.FileSharer;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;
    private final ScheduledExecutorService cleanScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024L; // 100MB limit

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port),0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(40);

        System.out.println("Upload directory is: " + this.uploadDir);

        File uploadDirFile = new File(uploadDir);
        if(!uploadDirFile.exists()){
            uploadDirFile.mkdirs();
        }

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download",new DownloadHandler());
        server.createContext("/",new CORSHandler());
        server.setExecutor(executorService);    
    }

    public void start(){
        server.start();
        System.out.println("API server startex on port " + server.getAddress().getPort());

        cleanScheduler.scheduleAtFixedRate(()->{
            try{
                System.out.println("Running periodic cleanup");
                cleanupOldFiles();
            }catch (Exception ex){
                System.err.println("Error during cleanup "+ ex.getMessage());
            }
        },30,30, TimeUnit.MINUTES);
    }

    public void stop(){
        server.stop(0);
        executorService.shutdown();

        cleanScheduler.shutdown();
        try{
            if(!cleanScheduler.awaitTermination(5,TimeUnit.SECONDS)){
                cleanScheduler.shutdownNow();
            }
        }catch (InterruptedException ex){
            cleanScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("API Server stopped");
    }

    private void cleanupOldFiles(){
        File dir = new File(uploadDir);
        if(!dir.exists() || !dir.isDirectory()){
            return;
        }

        File[] files = dir.listFiles();
        if(files == null) return;

        long now = System.currentTimeMillis();
        long cutoff = now - (5 * 60 * 1000L);


        for (File file : files) {
            if (file.isFile()) {
                try {
                    String canonicalPath = file.getCanonicalPath();
                    Long uploadTime = fileSharer.getUploadTime(canonicalPath);

                    if (uploadTime != null && uploadTime < cutoff) {
                        boolean deleted = file.delete();
                        if (deleted) {
                            System.out.println("Deleted old file: " + file.getName());
                            fileSharer.removeFilePath(canonicalPath);
                            fileSharer.removeUploadTimestamp(canonicalPath);
                        } else {
                            System.out.println("Failed to delete: " + file.getName());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error handling file during cleanup: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private class CORSHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin","*");
            headers.add("Access-Control-Allow-Methods","GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers","Content-Type,Authorization,X-Requested-With");

            if(exchange.getRequestMethod().equals("OPTIONS")){
                exchange.sendResponseHeaders(204,-1);
                return;
            }
            String response = "NOT FOUND";
            exchange.sendResponseHeaders(404,response.getBytes().length);
            try(OutputStream oos = exchange.getResponseBody()){
                oos.write(response.getBytes());
            }
        }
    }
//    POST /upload HTTP/1.1
//    Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryABC123 \r\n
//
//    ------WebKitFormBoundaryABC123
//    Content-Disposition: form-data; name="message"
//
//    Hello P2P
//
//    ------WebKitFormBoundaryABC123
//    Content-Disposition: form-data; name="myFile"; filename="hello.txt"
//    Content-Type: text/plain
//
//    This is the content of the file.
//
//    ------WebKitFormBoundaryABC123--




    private static class MultiParser{

        private final byte[] data;
        private final String boundary;
        public MultiParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse(){
            try{
                byte[] headerSeparator = {13,10,13,10};
                int headerEndIndex = findSequence(data,headerSeparator,0);
                if(headerEndIndex==-1){
                    System.err.println("MultipartParser: Header separator (\\r\\n\\r\\n) not found.");
                    return null;
                }

                int contentStartIndex = headerEndIndex + headerSeparator.length;

                String headers = new String(data,0,headerEndIndex);

                String filename = "unnamed-file";
                String filenameMarker = "filename=\"";
                int filenameStart = headers.indexOf(filenameMarker);
                if (filenameStart != -1) {
                    filenameStart += filenameMarker.length();
                    int filenameEnd = headers.indexOf("\"", filenameStart);
                    if (filenameEnd != -1) {
                        filename = headers.substring(filenameStart, filenameEnd);
                    }
                }

                String contentType = "application/octet-stream";
                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = headers.indexOf(contentTypeMarker);
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = headers.indexOf("\r\n", contentTypeStart);
                    if (contentTypeEnd != -1) {
                        contentType = headers.substring(contentTypeStart, contentTypeEnd).trim();
                    } else {
                        contentType = headers.substring(contentTypeStart).trim();
                    }
                }

                byte[] finalBoundarySeparator = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data,finalBoundarySeparator,contentStartIndex);

                if(contentEnd == -1){
                    byte[] partBoundarySeparator = ("\r\n--" + boundary).getBytes();
                    contentEnd = findSequence(data,partBoundarySeparator,contentStartIndex);
                }

                if(contentEnd == -1 || contentEnd <= contentStartIndex){
                    System.err.println("MultipartParser: Content end boundary not found.");
                    return null;
                }

                byte[] fileContent = Arrays.copyOfRange(data, contentStartIndex, contentEnd);
                return new ParseResult(filename,contentType,fileContent);
            }catch (Exception ex){
                System.err.println("Error parsing multipart data: " + ex.getMessage());
                return null;
            }
        }

        private int findSequence(byte[] data, byte[] sequence, int startPos){
            outer:
            for(int i = startPos; i<= data.length - sequence.length; i++){
                for(int j=0;j<sequence.length;j++){
                    if(data[i+j] != sequence[j]){
                        continue outer;
                    }
                }
                return i;
            }
            return  -1;
        }

        public static class ParseResult {
            public final String filename;
            public final String contentType;
            public final byte[] fileContent;

            public ParseResult(String filename, String contentType, byte[] fileContent) {
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
            }
        }
    }

    private class UploadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin","*");
            headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With");

            if(exchange.getRequestMethod().equals("OPTIONS")){
                exchange.sendResponseHeaders(204,-1);
                return;
            }

            if(!exchange.getRequestMethod().equalsIgnoreCase("POST")){
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405,response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
                return;
            }

            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");

            if(contentType == null || !contentType.startsWith("multipart/form-data")){
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400,response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
                return;
            }

            try{
                String boundary = contentType.substring(contentType.indexOf("boundary=")+9);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(),baos);
                byte[] requestData = baos.toByteArray();

                MultiParser parser = new MultiParser(requestData,boundary);
                MultiParser.ParseResult result = parser.parse();

                if(result == null){
                    String response = "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(400,response.getBytes().length);
                    try(OutputStream os = exchange.getResponseBody()){
                        os.write(response.getBytes());
                    }
                    return;
                }

                // Check file size
                if(result.fileContent.length > MAX_FILE_SIZE){
                    String response = "File too large. Maximum size is 100MB";
                    exchange.sendResponseHeaders(413,response.getBytes().length);
                    try(OutputStream os = exchange.getResponseBody()){
                        os.write(response.getBytes());
                    }
                    return;
                }

                String filename = result.filename;
                if(filename == null || filename.trim().isEmpty()){
                    filename = "unnamed-file";
                }

                String uniqueFilename = UUID.randomUUID().toString() + "_" + new File(filename).getName();
                String filePath = uploadDir + File.separator + uniqueFilename;

                try(FileOutputStream fos = new FileOutputStream(filePath)){
                    fos.write(result.fileContent);
                }

                int port = fileSharer.offerFile(filePath);


                new Thread(() -> fileSharer.startFileServer(port)).start();

                String jsonResponse = "{\"port\": " + port + "}";
                headers.add("Content-Type","application/json");
                exchange.sendResponseHeaders(200,jsonResponse.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(jsonResponse.getBytes());
                }
            }catch (Exception ex){
                System.err.println("Error processing file upload: " + ex.getMessage());
                String response = "Server error: " + ex.getMessage();
                exchange.sendResponseHeaders(500,response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
            }
        }
    }

    private  class DownloadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Method not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }


            String path = exchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf('/') + 1);

            try {
                int port = Integer.parseInt(portStr);

                try (Socket socket = new Socket("localhost", port);
                     InputStream socketInput = socket.getInputStream()) {

                    File tempFile = File.createTempFile("download-",".tmp");
                    String filename = "downloaded-file";
                    long expectedSize = 0;

                    try(FileOutputStream fos = new FileOutputStream(tempFile)){
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        // Read header lines
                        ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
                        
                        // Read header lines (Filename and Size)
                        int lineCount = 0;
                        while(lineCount < 2) {
                            ByteArrayOutputStream lineBaos = new ByteArrayOutputStream();
                            int b;
                            while((b = socketInput.read())!=-1){
                                if(b == '\n') break;
                                // lineBaos.write(b);
                                if(b != '\r')    lineBaos.write(b);
                            }
                            
                            // String line = lineBaos.toString().trim();
                            String line = new String(lineBaos.toByteArray(), "UTF-8").trim(); 
                            if(line.startsWith("Filename: ")){
                                filename = line.substring("Filename: ".length());
                            } else if(line.startsWith("Size: ")){
                                try {
                                    expectedSize = Long.parseLong(line.substring("Size: ".length()));
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid file size in header: " + line);
                                }
                            }
                            lineCount++;
                        }
                        
                        if(filename.equals("downloaded-file")){
                            tempFile.delete();
                            String response = "File no longer available";
                            exchange.sendResponseHeaders(404, response.getBytes().length);
                            try(OutputStream os = exchange.getResponseBody()){
                                os.write(response.getBytes());
                            }
                            return;
                        }

                        long totalBytesRead = 0;
                        int chunkCount = 0;
                        
                        System.out.println("Starting chunked download of " + filename + " (expected: " + expectedSize + " bytes)");
                        
                        while((bytesRead = socketInput.read(buffer)) != -1){
                            fos.write(buffer,0,bytesRead);
                            totalBytesRead += bytesRead;
                            chunkCount++;
                            
                            // Log progress every 10 chunks or when complete
                            if (chunkCount % 10 == 0 || totalBytesRead == expectedSize) {
                                double progress = expectedSize > 0 ? (double) totalBytesRead / expectedSize * 100 : 0;
                                System.out.printf("Download %s: %d/%d bytes received (%.1f%%) - %d chunks%n", 
                                    filename, totalBytesRead, expectedSize, progress, chunkCount);
                            }
                        }
                        
                        System.out.println("Download completed: " + filename + " (" + totalBytesRead + " bytes in " + chunkCount + " chunks)");

                        if(totalBytesRead == 0) {
                            tempFile.delete();
                            String response = "File no longer available";
                            exchange.sendResponseHeaders(404, response.getBytes().length);
                            try(OutputStream os = exchange.getResponseBody()){
                                os.write(response.getBytes());
                            }
                            return;
                        }
                    }

                    headers.add("Content-Disposition","attachment; filename=\"" + filename + "\"");
                    headers.add("Content-Type","application/octet-stream");

                    exchange.sendResponseHeaders(200, tempFile.length());
                    try(OutputStream os = exchange.getResponseBody();
                        FileInputStream fis = new FileInputStream(tempFile)){
                        byte[] buffer = new byte[4096];
                        int bytesread;
                        while((bytesread = fis.read(buffer))!=-1){
                            os.write(buffer,0,bytesread);
                        }
                    }
                    tempFile.delete();
                }catch (IOException e){
                    System.err.println("Error downloading file from peer: " + e.getMessage());
                    String response = "Error downloading file: " + e.getMessage();
                    exchange.sendResponseHeaders(500,response.getBytes().length);
                    try(OutputStream os = exchange.getResponseBody()){
                        os.write(response.getBytes());
                    }
                }

            } catch (NumberFormatException e) {
                String response = "Bad response: Invalid Port number";
                exchange.sendResponseHeaders(400,response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
            }
        }
    }
}
