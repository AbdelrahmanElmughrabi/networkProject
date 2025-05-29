// TCPServer.java
// Handles server registration, client requests, and communication with the load balancer

import java.io.*; // For input/output streams
import java.net.*; // For networking (Socket, ServerSocket)

public class TCPServer {

    // Main method: Entry point for the TCPServer application
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java TCPServer <port> <strategy>");
            return;
        }
        int myPort = Integer.parseInt(args[0]); // Server's port number
        String strategy = args[1]; // Server's load balancing strategy

        // Connect to the load balancer and register this server
        Socket lbSocket = new Socket("localhost", 6789);
        DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
        outToLb.writeBytes("JOIN " + myPort + " " + strategy + "\n"); // Send JOIN message
        outToLb.flush();
        BufferedReader inFromLb = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
        String response = inFromLb.readLine(); // Read response from load balancer
        if (!response.equals("OK")) {
            System.out.println("Failed to join load balancer");
            lbSocket.close();
            return;
        }

        // Start listening for client requests
        ServerSocket serverSocket = new ServerSocket(myPort);
        System.out.println("Server listening on port " + myPort);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept(); // Accept client connection
                new Thread(new RequestHandler(clientSocket, lbSocket)).start(); // Handle request in new thread
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                e.printStackTrace();
            }
        }
        serverSocket.close();
        lbSocket.close();
    }

    // Handles individual client requests
    static class RequestHandler implements Runnable {

        private Socket clientSocket; // Client connection socket
        private Socket lbSocket;     // Load balancer connection socket

        public RequestHandler(Socket clientSocket, Socket lbSocket) {
            this.clientSocket = clientSocket;
            this.lbSocket = lbSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); // Read from client
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream()); // Write to client
                String choiceStr = inFromClient.readLine(); // Read request type
                String type = inFromClient.readLine(); // Read specific request
                if (choiceStr == null || type == null) {
                    outToClient.writeBytes("Invalid request\n");
                    outToClient.flush();
                    return;
                }
                int choice = Integer.parseInt(choiceStr); // Parse request type

                String response = processRequest(choice, type); // Process the request
                outToClient.writeBytes(response + "\n"); // Send response to client
                outToClient.flush();

                DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream()); // Notify load balancer
                outToLb.writeBytes("FREE\n"); // Indicate server is free
                outToLb.flush();
                System.out.println("Sent FREE from server handling port " + clientSocket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close(); // Close client connection
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Processes the client's request based on the type
        private String processRequest(int choice, String type) {
            try {
                if (choice == 1) { // Directory listing
                    File dir = new File(type);
                    if (dir.isDirectory()) {
                        StringBuilder result = new StringBuilder();
                        for (File file : dir.listFiles()) {
                            result.append(file.getName()).append("\n");
                        }
                        return result.toString();
                    }
                    return "Invalid directory\n";
                } else if (choice == 2) { // File transfer
                    File file = new File(type);
                    if (file.isFile()) {
                        StringBuilder content = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                content.append(line).append("\n");
                            }
                        }
                        return content.toString();
                    }
                    return "File not found\n";
                } else if (choice == 3) { // Computation (simulated by sleep)
                    int duration = Integer.parseInt(type);
                    Thread.sleep(duration * 1000);
                    return "Computation done\n";
                } else if (choice == 4) { // Video streaming (simulated by sending frames)
                    int duration = Integer.parseInt(type);
                    StringBuilder stream = new StringBuilder();
                    for (int i = 0; i < duration; i++) {
                        stream.append("Video frame ").append(i).append("\n");
                        Thread.sleep(1000);
                    }
                    return stream.toString();
                } else {
                    return "Invalid choice\n";
                }
            } catch (Exception e) {
                return "Error processing request: " + e.getMessage() + "\n";
            }
        }
    }
}
