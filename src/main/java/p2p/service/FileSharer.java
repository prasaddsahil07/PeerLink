package p2p.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {
    private HashMap<Integer, String> availableFiles;

    public FileSharer(){
        availableFiles = new HashMap<>();
    }

    // offer a port to the file
    public int offerFile(String filePath){
        int port;       // to offer a file, we have to make sure that atleast one port is available
        while (true) {          // handle timeout later
            port = UploadUtils.generateCode();
            if(!availableFiles.containsKey(port)){
                availableFiles.put(port, filePath);
                return port;
            }   
        }
    }

    // once the user hits the fileDownloader then after parsing and all , the server will hit the fileSharer
    public void startFileServer(int port){      // one thread is responsible to share a file on the network
        String filePath = availableFiles.get(port);

        if(filePath == null){
            System.out.println("NO File is associated with port: " + port);
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)){   // open a socket on the required port
            System.out.println("Servinf file "+ new File(filePath).getName() + " on port "+port);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connection: "+ clientSocket.getInetAddress());
            new Thread(new FileSenderHandler(clientSocket, filePath)).start();  // thread invokes runnable i.e., the run method inside runnable
        } catch (Exception e) {
            System.err.println("ErrorHandling file server on port: "+port);
        }
    }

    private static class FileSenderHandler implements  Runnable{
    
        private final Socket clientSocket;
        private final String filePath;

        FileSenderHandler(Socket clientSocket, String filePath){
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }

        @Override
        public void run(){
            try (FileInputStream fis = new FileInputStream(filePath)){
                OutputStream oos = clientSocket.getOutputStream();  // raw data sent to socket
                String fileName = new File(filePath).getName();
                String header = "Filename: "+fileName+"\n";

                oos.write(header.getBytes());

                // read all the content from the file and send on the socket
                byte[] buffer = new byte[4096];
                int byteRead;
                while((byteRead = fis.read(buffer)) != -1){
                    oos.write(buffer, 0, byteRead);
                }
                System.out.println("File "+ fileName + " sent to "+ clientSocket.getInetAddress());
            } catch (Exception e) {
                System.err.println("Error while sending file to the client: "+e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    System.err.println("Error while closing the client socket: "+e.getMessage());
                }
            }
        }
    }


}
