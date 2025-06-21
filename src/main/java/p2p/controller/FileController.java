package p2p.controller;

import java.io.File;
import java.util.concurrent.ExecutorService;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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

    public class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if (exchange.getRequestMethod().equals("OPTIONS")) {    // only OPTIONS method is handled by CORSHandler
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

}
