package p2p.service;

import p2p.utils.UploadUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileSharer {
    private HashMap<Integer,String> availableFiles;
    private final HashMap<String, Long> fileUploadTimes = new HashMap<>();


    public FileSharer(){
         availableFiles = new HashMap<>();
     }

     public int offerFile(String filePath){
         int port;
         while(true){
             port = UploadUtils.generateCode();
             if(!availableFiles.containsKey(port)){
                 availableFiles.put(port,filePath);
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

     public void startFileServer(int port){
         String filePath = availableFiles.get(port);
         if(filePath==null){
             System.out.println("No file is associated with this port: " + port);
             return;
         }

         try(ServerSocket serverSocket = new ServerSocket(port)){
             System.out.println("Serving File "+ new File(filePath).getName());
             Socket clientSocket = serverSocket.accept();
             System.out.println("Client Connection: " + clientSocket.getInetAddress());
             new Thread(new FileSenderHandler(clientSocket,filePath,this)).start();
         }catch (Exception ex){
             System.out.println("Error handling file server on port: " + port);
         }
     }

    public void removeFilePath(String path){
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
            // Ignore
        }
    }

    private static  class FileSenderHandler implements  Runnable{
         private final Socket clientSocket;
         private final String filePath;
         private final FileSharer fileSharer;

         public FileSenderHandler(Socket clientSocket, String filePath, FileSharer fileSharer) {
             this.clientSocket = clientSocket;
             this.filePath = filePath;
             this.fileSharer = fileSharer;
         }

         @Override
         public void run(){
             try {
                 // Check if file exists FIRST
                 File file = new File(filePath);
                 if (!file.exists()) {
                     System.err.println("File no longer exists: " + filePath);
                     return; // Don't send anything
                 }

                 // Now safely create FileInputStream
                 try(FileInputStream fis = new FileInputStream(filePath)){
                     OutputStream oos = clientSocket.getOutputStream();

                     String fileName = file.getName();
                     String header  = "Filename: " + fileName + "\n";
                     oos.write(header.getBytes());

                     byte[] buffer = new byte[4096];
                     int byteRead;
                     while((byteRead = fis.read(buffer)) != -1){
                         oos.write(buffer,0,byteRead);
                     }
                     System.out.println("File " + fileName + " sent to " + clientSocket.getInetAddress());

                     // Delete after successful send
                     if (file.delete()) {
                         System.out.println("File deleted successfully: " + fileName);
                         fileSharer.removeFilePath(filePath);
                         fileSharer.removeUploadTimestamp(filePath);
                     } else {
                         System.err.println("Failed to delete file: " + fileName);
                     }
                 }
             }catch (Exception ex){
                 System.err.println("Error sending file to the client " + ex.getMessage() );
             }finally {
                 try {
                     clientSocket.close();
                 }catch(Exception ex){
                     System.err.println("Error closing socket: " + ex.getMessage());
                 }
             }
         }

    }
}