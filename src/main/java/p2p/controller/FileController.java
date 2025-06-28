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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import p2p.service.FileSharer;

public class FileController {

    private final FileSharer fileSharer;    // fileController is having "has-a" relation with fileSharer
    private final HttpServer server;    // built in package (no use of framework)
    private final String uploadDir;        // tempDir where the server stores the file uploaded by client
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tempdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdir();
        }

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());   // for health check purpose
        server.setExecutor(executorService);        // allowing server to use the threadpool
    }

    public void start() {
        server.start();
        System.out.println("API Server started on port: " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API Server stopped");
    }

    private class CORSHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {    // only OPTIONS method is handled by CORSHandler
                exchange.sendResponseHeaders(204, -1);      // status code for : no content
                return;
            }

            String response = "NOT FOUND";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream oos = exchange.getResponseBody()) {   // as try block ends, the stram automatically closes
                oos.write(response.getBytes());
            }
        }
    }

    private class UploadHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Methos not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);

                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
                return;
            }

            Headers responseHeaders = exchange.getRequestHeaders();
            String contentType = responseHeaders.getFirst("Content-Type");

            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must ne form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);

                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
                return;
            }

            // if everything is correct, then do parsing
            try {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);    // bounadry is present in http request header
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();

                // now call multiparser to parse the request data
                Multiparser parser = new Multiparser(requestData, boundary);
                Multiparser.ParseResult result = parser.parse();   // this will return fileName and fileContent

                if (result == null) {
                    String response = "Bad request: Could not parse file content";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream oos = exchange.getResponseBody()) {
                        oos.write(response.getBytes());
                    }
                    return;
                }

                String fileName = result.fileName;
                if (fileName == null || fileName.trim().isEmpty()) {
                    fileName = "unnamed-file";
                }
                String uniqueFileName = UUID.randomUUID().toString() + "_" + new File(fileName).getName();
                String filePath = uploadDir + File.separator + uniqueFileName;

                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(result.fileContent);  // temporarily write the file content on the server
                }

                // fileSharer will offer a port to the user who uploaded the file
                int port = fileSharer.offerFile(filePath);  // providing the absolute path to the fileSharer
                new Thread(() -> fileSharer.startFileServer(port)).start(); // thread handles a sharing task
                String jsonResponse = "{\"port\": " + port + "}";   // sending json format after stringify
                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(jsonResponse.getBytes());
                }

            } catch (Exception e) {
                System.err.println("Error processing file upload: " + e.getMessage());
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
            }

        }
    }

    private class DownloadHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Methos not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);

                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
                return;
            }

            String path = exchange.getRequestURI().getPath();   // the url basically containing the port as param
            String portStr = path.substring(path.lastIndexOf('/' + 1));   // extracting the port number in string

            try {
                int port = Integer.parseInt(portStr);   // converting the port into int from string

                try (Socket socket = new Socket("localhost", port)) {
                    InputStream socketInput = socket.getInputStream();
                    File tempFile = File.createTempFile("download-", ".tmp");   // temporary file to hold data of file sent from fileSharer before sending back to the requested user
                    String fileName = "downloaded-file";

                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {   // reading the data from fileSharer to my temp file
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
                        int b;
                        while ((b = socketInput.read()) != -1) {
                            if (b == '\n') {
                                break;
                            }
                            headerBaos.write(b);
                        }
                        String header = headerBaos.toString().trim();

                        if (header.startsWith("Filename: ")) {
                            fileName = header.substring("Filename: ".length());
                        }

                        while ((bytesRead = socketInput.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    headers.add("Content-Disposition: ", "attachment: filename=\"" + fileName + "\"");
                    headers.add("Content-Type", "application/octet-stream");
                    exchange.sendResponseHeaders(200, tempFile.length());
                    try (OutputStream oos = exchange.getResponseBody()) {
                        FileInputStream fis = new FileInputStream(tempFile); // write the file content onto the socket to send the user
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            oos.write(buffer, 0, bytesRead);
                        }
                    }
                    tempFile.delete();
                }
            } catch (Exception e) {
                System.out.println("Error downloading the file: "+e.getMessage());
                String response = "Error downloading file "+e.getMessage();
                headers.add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
            }
        }
    }

    // for multi parser only, we are not using spring boot or any framework so that can implement on our own
    private static class Multiparser {

        private final byte[] data;
        private final String boundary;

        public Multiparser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse() {
            try {
                String dataAsString = new String(data); // convert the byte array data into string, only pdf or text file (not blob object)
                String fileNameMarker = "fileName=\"";
                int filenameStart = dataAsString.indexOf(fileNameMarker);
                if (filenameStart == -1) {
                    return null;    // filenameStart doesn't exist
                }

                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String fileName = dataAsString.substring(filenameStart, filenameEnd);

                String contentTypeMarker = "Content-Type: ";

                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);
                String contentType = "application/octet-stream";
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = dataAsString.indexOf("\r\n", contentTypeStart);
                    contentType = dataAsString.substring(contentTypeStart, contentTypeEnd);
                }

                String headerEndMarker = "\r\n\r\n";        // it is standart in HTTP1.1
                int headerEnd = dataAsString.indexOf(headerEndMarker);
                if (headerEnd == -1) {
                    return null;
                }

                int contentStart = headerEnd + headerEndMarker.length();

                byte[] boundaryBytes = ("\r\n--" + boundary + "--").getBytes();
                int contentEnd = findSequence(data, boundaryBytes, contentStart);

                if (contentEnd == -1) {
                    boundaryBytes = ("\r\n--" + boundary).getBytes();     // try to find the end without "--"
                    contentEnd = findSequence(data, boundaryBytes, contentStart);
                }

                if (contentEnd == -1) {
                    return null;
                }

                byte[] fileContent = new byte[contentEnd - contentStart];
                System.arraycopy(data, contentStart, fileContent, 0, fileContent.length);
                return new ParseResult(fileName, fileContent, contentType);
            } catch (Exception e) {
                System.out.println("Error parsing multi-part data: " + e.getMessage());
                return null;
            }
        }

        public static class ParseResult {

            public final String fileName;
            public final byte[] fileContent;
            public final String contentType;

            public ParseResult(String fileName, byte[] fileContent, String contentType) {
                this.fileName = fileName;
                this.fileContent = fileContent;
                this.contentType = contentType;
            }
        }

        private static int findSequence(byte[] data, byte[] sequence, int startPos) {
            // the whole purpose of this code is that , go through the entire content and find the occurence of particular word that's it
            outer:
            for (int i = 0; i <= data.length - sequence.length; i++) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;     // outer is a way to write a recursive code in java
                    }
                }
                return i;
            }
            return -1;
        }
    }
}
