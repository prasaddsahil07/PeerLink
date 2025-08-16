package p2p;

import java.io.IOException;

import p2p.controller.FileController;

public class App {

    public static void main(String[] args) {
        try {
            int port = 8080;

            // Read cloud service's PORT env variable if available
            String portEnv = System.getenv("PORT");
            if (portEnv != null) {
                try {
                    port = Integer.parseInt(portEnv);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid PORT env var, falling back to 8080");
                }
            }

            FileController fileController = new FileController(port);
            fileController.start();

            System.out.println("PeerLink server started on port " + port);
            System.out.println("UI available at http://localhost:3000 (for local dev)");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                fileController.stop();
            }));

            if (portEnv == null) {
                // Local mode: wait for Enter to stop
                System.out.println("Press Enter to stop the server...");
                System.in.read();
            } else {
                // Render/Prod mode: keep thread alive
                Thread.currentThread().join();
            }

        } catch (IOException | InterruptedException ex) {
            System.err.println("Error starting server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}