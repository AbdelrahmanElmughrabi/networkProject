import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

// TestMain Class to Orchestrate Testing
public class TestMain {

    public static void main(String[] args) throws Exception {
        // Step 1: Create temporary directory and file
        File tempDir = Files.createTempDirectory("testDir").toFile();
        File tempFile = new File(tempDir, "test.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Test content\n");
        }
        String dirPath = tempDir.getAbsolutePath();
        String filePath = tempFile.getAbsolutePath();

        // Step 2: Start the load balancer
        System.out.println("Starting load balancer with static strategy on port 6789...");
        Thread lbThread = new Thread(() -> {
            try {
                LoadBalancer.main(new String[]{"static"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        lbThread.start();

        // Step 3: Start two servers
        System.out.println("Starting server 1 on port 7000 with static strategy...");
        Thread server1Thread = new Thread(() -> {
            try {
                TCPServer.main(new String[]{"7000", "static"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server1Thread.start();

        System.out.println("Starting server 2 on port 7001 with dynamic strategy...");
        Thread server2Thread = new Thread(() -> {
            try {
                TCPServer.main(new String[]{"7001", "dynamic"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server2Thread.start();

        // Step 4: Wait for servers to register
        System.out.println("Waiting for servers to register...");
        Thread.sleep(5000);

        // Step 5: Test all request types
        System.out.println("\n=== Testing Request Types ===");

        // Directory Listing
        System.out.println("Sending directory listing request...");
        try {
            String dirResponse = Client.runRequest(1, dirPath);
            System.out.println("Directory listing response: " + dirResponse);
        } catch (IOException e) {
            System.out.println("Directory listing error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // File Transfer
        System.out.println("Sending file transfer request...");
        try {
            String fileResponse = Client.runRequest(2, filePath);
            System.out.println("File transfer response: " + fileResponse);
        } catch (IOException e) {
            System.out.println("File transfer error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // Computation
        System.out.println("Sending computation request for 5 seconds...");
        try {
            String compResponse = Client.runRequest(3, "5");
            System.out.println("Computation response: " + compResponse);
        } catch (IOException e) {
            System.out.println("Computation error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // Video Streaming
        System.out.println("Sending video streaming request for 3 seconds...");
        try {
            String streamResponse = Client.runRequest(4, "3");
            System.out.println("Video streaming response: " + streamResponse);
        } catch (IOException e) {
            System.out.println("Video streaming error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // Step 6: Demonstrate load balancing with concurrent requests
        System.out.println("\n=== Testing Load Balancing ===");
        System.out.println("Sending two concurrent directory listing requests...");
        Thread client1 = new Thread(() -> {
            try {
                String resp1 = Client.runRequest(1, dirPath);
                System.out.println("First concurrent response: " + resp1);
            } catch (IOException e) {
                System.out.println("First concurrent error: " + e.getMessage());
            }
        });
        Thread client2 = new Thread(() -> {
            try {
                String resp2 = Client.runRequest(1, dirPath);
                System.out.println("Second concurrent response: " + resp2);
            } catch (IOException e) {
                System.out.println("Second concurrent error: " + e.getMessage());
            }
        });
        client1.start();
        client2.start();
        client1.join();
        client2.join();

        // Step 7: Test edge case - invalid directory
        System.out.println("\n=== Testing Edge Case: Invalid Directory ===");
        try {
            String invalidResponse = Client.runRequest(1, "/non/existent/path");
            System.out.println("Invalid directory response: " + invalidResponse);
        } catch (IOException e) {
            System.out.println("Invalid directory error: " + e.getMessage());
        }

        // Step 8: Test dynamic strategy
        System.out.println("\n=== Testing Dynamic Load Balancing Strategy ===");
        System.out.println("Restarting load balancer with dynamic strategy...");
        lbThread.interrupt();
        try {
            lbThread.join(); // Wait for the old load balancer to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Old load balancer stopped.");

        // Start new load balancer with dynamic strategy
        Thread newLbThread = new Thread(() -> {
            try {
                LoadBalancer.main(new String[]{"dynamic"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        newLbThread.start();
        lbThread = newLbThread; // Update reference

        // Wait for servers to re-register
        Thread.sleep(5000);
        System.out.println("Sending directory listing request with dynamic strategy...");
        try {
            String dynamicResponse = Client.runRequest(1, dirPath);
            System.out.println("Dynamic strategy response: " + dynamicResponse);
        } catch (IOException e) {
            System.out.println("Dynamic strategy error: " + e.getMessage());
        }

        // Step 9: Note on dynamic server management
        System.out.println("\n=== Dynamic Server Management ===");
        System.out.println("Note: Server deregistration via 'GOODBYE' is supported. To test, modify TCPServer to send 'GOODBYE' after a request.");

        // Step 10: Clean up temporary files
        System.out.println("\n=== Cleaning Up ===");
        if (tempFile.exists()) {
            tempFile.delete();
        }
        if (tempDir.exists()) {
            tempDir.delete();
        }

        // Step 11: Keep system running for observation
        System.out.println("\n=== System Running ===");
        System.out.println("Press Ctrl+C to stop the demonstration.");
        while (true) {
            Thread.sleep(1000);
        }
    }
}

// Updated Client Class
class Client {

    public static void main(String[] args) throws IOException {
        int choice;
        String type;
        if (args.length >= 2) {
            choice = Integer.parseInt(args[0]);
            type = args[1];
        } else {
            Scanner scn = new Scanner(System.in);
            System.out.println("Choose your request type:\n1. Directory listing\n2. File transfer\n3. Computation\n4. Video streaming");
            choice = scn.nextInt();
            scn.nextLine();
            System.out.println("Enter your specific request:");
            type = scn.nextLine();
        }
        String response = runRequest(choice, type);
        System.out.println("From Server: " + response);
    }

    public static String runRequest(int choice, String type) throws IOException {
        StringBuilder response = new StringBuilder();
        int serverPort = getServerPort(choice);

        try (Socket serverSocket = new Socket("localhost", serverPort)) {
            DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            outToServer.writeBytes(choice + "\n");
            outToServer.writeBytes(type + "\n");
            outToServer.flush();

            String line;
            while ((line = inFromServer.readLine()) != null) {
                response.append(line).append("\n");
                System.out.println("From Server: " + line);
            }
        }
        return response.toString();
    }

    private static int getServerPort(int choice) throws IOException {
        try (Socket lbSocket = new Socket("localhost", 6789)) {
            DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
            BufferedReader inFromLb = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));

            outToLb.writeBytes("REQUEST " + choice + "\n");
            outToLb.flush();
            String portStr = inFromLb.readLine();
            if (portStr.equals("NO_SERVER")) {
                throw new IOException("No server available");
            }
            int serverPort = Integer.parseInt(portStr);
            System.out.println("Assigned to server port: " + serverPort);
            return serverPort;
        }
    }
}

// LoadBalancer Class
class LoadBalancer {

    private static List<Server> servers = new ArrayList<>();
    private static String strategy;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java LoadBalancer <strategy>");
            return;
        }
        strategy = args[0];
        int port = 6789;

        int maxRetries = 5;
        int retryDelay = 1000; // 1 second
        ServerSocket serverSocket = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                serverSocket = new ServerSocket(port);
                break;
            } catch (IOException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                System.out.println("Failed to bind to port " + port + ", retrying in " + retryDelay / 1000 + " seconds...");
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry binding", ie);
                }
            }
        }
        if (serverSocket == null) {
            System.out.println("Failed to start load balancer after " + maxRetries + " retries.");
            return;
        }
        System.out.println("Load Balancer listening on port " + port);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(new ConnectionHandler(socket)).start();
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                e.printStackTrace();
            }
        }
        System.out.println("Load Balancer shutting down...");
        serverSocket.close();
        System.out.println("Load Balancer stopped.");
    }

    static class ConnectionHandler implements Runnable {

        private Socket socket;

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String firstMessage = in.readLine();
                if (firstMessage != null && firstMessage.startsWith("JOIN")) {
                    handleServerJoin(firstMessage, socket);
                } else if (firstMessage != null && firstMessage.startsWith("REQUEST")) {
                    handleClientRequest(firstMessage, socket);
                } else {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleServerJoin(String message, Socket socket) throws IOException {
        String[] parts = message.split(" ");
        if (parts.length == 3 && parts[0].equals("JOIN")) {
            int serverPort = Integer.parseInt(parts[1]);
            String serverStrategy = parts[2];
            Server server = new Server(serverPort, serverStrategy, false, socket);
            synchronized (servers) {
                servers.add(server);
            }
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes("OK\n");
            out.flush();
            new Thread(new ServerStatusHandler(server)).start();
        } else {
            socket.close();
        }
    }

    private static void handleClientRequest(String message, Socket clientSocket) throws IOException {
        String[] parts = message.split(" ");
        if (parts.length == 2 && parts[0].equals("REQUEST")) {
            int choice = Integer.parseInt(parts[1]);
            Server selectedServer = selectServer();
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            if (selectedServer != null) {
                selectedServer.isBusy = true;
                out.writeBytes(selectedServer.port + "\n");
            } else {
                System.out.println("No free servers available for request type " + choice);
                out.writeBytes("NO_SERVER\n");
            }
            out.flush();
        }
        clientSocket.close();
    }

    private static Server selectServer() {
        synchronized (servers) {
            List<Server> freeServers = new ArrayList<>();
            for (Server server : servers) {
                if (!server.isBusy) {
                    freeServers.add(server);
                }
            }
            if (freeServers.isEmpty()) {
                System.out.println("No free servers found");
                return null;
            }
            if (strategy.equals("static")) {
                return freeServers.get(0);
            } else if (strategy.equals("dynamic")) {
                Server leastUsed = freeServers.get(0);
                for (int i = 1; i < freeServers.size(); i++) {
                    if (freeServers.get(i).lastFreeTime < leastUsed.lastFreeTime) {
                        leastUsed = freeServers.get(i);
                    }
                }
                return leastUsed;
            }
        }
        return null;
    }

    static class ServerStatusHandler implements Runnable {

        private Server server;

        public ServerStatusHandler(Server server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(server.socket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("FREE")) {
                        synchronized (servers) {
                            server.isBusy = false;
                            server.lastFreeTime = System.currentTimeMillis();
                            System.out.println("Received FREE from server " + server.port);
                        }
                    } else if (message.equals("GOODBYE")) {
                        synchronized (servers) {
                            servers.remove(server);
                            System.out.println("Server " + server.port + " deregistered");
                        }
                        server.socket.close();
                        break;
                    }
                }
                synchronized (servers) {
                    servers.remove(server);
                    System.out.println("Server " + server.port + " disconnected");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Server {

    int port;
    String strategy;
    boolean isBusy;
    Socket socket;
    long lastFreeTime;

    public Server(int port, String strategy, boolean isBusy, Socket socket) {
        this.port = port;
        this.strategy = strategy;
        this.isBusy = isBusy;
        this.socket = socket;
        this.lastFreeTime = System.currentTimeMillis();
    }
}

// TCPServer Class
class TCPServer {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java TCPServer <port> <strategy>");
            return;
        }
        int myPort = Integer.parseInt(args[0]);
        String strategy = args[1];

        Socket lbSocket = new Socket("localhost", 6789);
        DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
        outToLb.writeBytes("JOIN " + myPort + " " + strategy + "\n");
        outToLb.flush();
        BufferedReader inFromLb = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
        String response = inFromLb.readLine();
        if (!response.equals("OK")) {
            System.out.println("Failed to join load balancer");
            lbSocket.close();
            return;
        }

        ServerSocket serverSocket = new ServerSocket(myPort);
        System.out.println("Server listening on port " + myPort);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(new RequestHandler(clientSocket, lbSocket)).start();
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

    static class RequestHandler implements Runnable {

        private Socket clientSocket;
        private Socket lbSocket;

        public RequestHandler(Socket clientSocket, Socket lbSocket) {
            this.clientSocket = clientSocket;
            this.lbSocket = lbSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
                String choiceStr = inFromClient.readLine();
                String type = inFromClient.readLine();
                if (choiceStr == null || type == null) {
                    outToClient.writeBytes("Invalid request\n");
                    outToClient.flush();
                    return;
                }
                int choice = Integer.parseInt(choiceStr);

                String response = processRequest(choice, type);
                outToClient.writeBytes(response + "\n");
                outToClient.flush();

                DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
                outToLb.writeBytes("FREE\n");
                outToLb.flush();
                System.out.println("Sent FREE from server handling port " + clientSocket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String processRequest(int choice, String type) {
            try {
                if (choice == 1) {
                    File dir = new File(type);
                    if (dir.isDirectory()) {
                        StringBuilder result = new StringBuilder();
                        for (File file : dir.listFiles()) {
                            result.append(file.getName()).append("\n");
                        }
                        return result.toString();
                    }
                    return "Invalid directory\n";
                } else if (choice == 2) {
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
                } else if (choice == 3) {
                    int duration = Integer.parseInt(type);
                    Thread.sleep(duration * 1000);
                    return "Computation done\n";
                } else if (choice == 4) {
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