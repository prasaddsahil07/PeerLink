package p2p;

import java.io.IOException;

import p2p.controller.FileController;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        try{
            FileController fileController = new FileController(8080);
            fileController.start();

            System.out.println("PeerLink server started on port 8080");
            System.out.println("Ui available at http://localhost:3000");

            Runtime.getRuntime().addShutdownHook(new Thread(()-> {
                System.out.println("Shutting down server...");
                fileController.stop();
            }));

            System.out.println("Press enter to stop the server");
            System.in.read(); // does it work??
        }catch (IOException ex){
            System.err.println("Error starting server" + ex.getMessage());
            ex.printStackTrace();
        }
    }
}