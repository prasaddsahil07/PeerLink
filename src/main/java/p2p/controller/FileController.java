package p2p.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
        this.uploadDir = System.getProperty("java.io.tempdir")+File.separator+"peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);

        File uploadDirFile = new File(uploadDir);
        if(!uploadDirFile.exists()){
            uploadDirFile.mkdir();
        }

        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());   // for health check purpose
        server.setExecutor(executorService);        // allowing server to use the threadpool
    }

    public void start(){
        server.start();
        System.out.println("API Server started on port: " + server.getAddress().getPort());
    }

    public void stop(){
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

            if(!exchange.getRequestMethod().equalsIgnoreCase("POST")){
                String response = "Methos not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);

                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
                return;
            }

            Headers responseHeaders = exchange.getRequestHeaders();
            String contentType = responseHeaders.getFirst("Content-Type");

            if(contentType == null || !contentType.startsWith("multipart/form-data")){
                String response = "Bad Request: Content-Type must ne form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);

                try (OutputStream oos = exchange.getResponseBody()) {
                    oos.write(response.getBytes());
                }
                return;
            }

            // if everything is correct, then do parsing
            try {
                String boundary = contentType.substring(contentType.indexOf("boundary=")+9);    // bounadry is present in http request header
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();

                // now call multiparser to parse the request data
                Multiparser parser = new Multiparser(requestData, boundary);

            } catch (Exception e) {

            }

        }
    }


    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            if(!exchange.getRequestMethod().equalsIgnoreCase("POST")){
                String response = "Methos not allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);

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

        public Multiparser(byte[] data, String boundary){
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse () {
            try {
                String dataAsString = new String(data); // convert the byte array data into string, only pdf or text file (not blob object)
                String fileNameMarker = "fileName=\"";
                int filenameStart = dataAsString.indexOf(fileNameMarker);
                if(filenameStart == -1){
                    return null;    // filenameStart doesn't exist
                }

                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String fileName = dataAsString.substring(filenameStart, filenameEnd);

                String contentTypeMarker = "Content-Type: ";

                int contentTypeStart = dataAsString.indexOf(contentTypeMarker, filenameEnd);

                if(contentTypeStart != -1){
                    contentTypeStart += contentTypeMarker.length();
                }

            } catch (Exception e) {
                
            }
        }

    }


    public static class ParseResult {
        public final String fileName;
        public final byte[] fileContent;

        public ParseResult (String fileName, byte[] fileContent) {
            this.fileName = fileName;
            this.fileContent = fileContent;
        }
    }


}
