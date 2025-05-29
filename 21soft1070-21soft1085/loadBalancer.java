// LoadBalancer.java
// Handles server registration, client requests, and load balancing strategies

import java.io.*; // For input/output streams
import java.net.*; // For networking (Socket, ServerSocket, SocketException)
import java.util.*; // For List, ArrayList, etc.

public class LoadBalancer {

    private static List<Server> servers = new ArrayList<>(); // List of registered servers
    private static String strategy; // Load balancing strategy (static/dynamic)

    // Main method: Entry point for the LoadBalancer application
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java LoadBalancer <strategy>");
            return;
        }
        strategy = args[0]; // Set the load balancing strategy
        int port = 6789; // Load balancer listens on this port

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

        // Main loop: accept connections from servers and clients
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = serverSocket.accept(); // Accept connection
                new Thread(new ConnectionHandler(socket)).start(); // Handle connection in new thread
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

    // Handles incoming connections (from servers or clients)
    static class ConnectionHandler implements Runnable {

        private Socket socket;

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String firstMessage = in.readLine(); // Read the first message
                if (firstMessage != null && firstMessage.startsWith("JOIN")) {
                    handleServerJoin(firstMessage, socket); // Handle server registration
                } else if (firstMessage != null && firstMessage.startsWith("REQUEST")) {
                    handleClientRequest(firstMessage, socket); // Handle client request
                } else {
                    socket.close(); // Close invalid connections
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Handles server registration (JOIN message)
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
            new Thread(new ServerStatusHandler(server)).start(); // Start handler for server status updates
        } else {
            socket.close();
        }
    }

    // Handles client requests (REQUEST message)
    private static void handleClientRequest(String message, Socket clientSocket) throws IOException {
        String[] parts = message.split(" ");
        if (parts.length == 2 && parts[0].equals("REQUEST")) {
            int choice = Integer.parseInt(parts[1]);
            Server selectedServer = selectServer(); // Select a server based on strategy
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            if (selectedServer != null) {
                selectedServer.isBusy = true;
                out.writeBytes(selectedServer.port + "\n"); // Send server port to client
            } else {
                System.out.println("No free servers available for request type " + choice);
                out.writeBytes("NO_SERVER\n"); // Indicate no server is available
            }
            out.flush();
        }
        clientSocket.close();
    }

    // Selects a server based on the current load balancing strategy
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
                return freeServers.get(0); // Always pick the first free server
            } else if (strategy.equals("dynamic")) {
                Server leastUsed = freeServers.get(0);
                for (int i = 1; i < freeServers.size(); i++) {
                    if (freeServers.get(i).lastFreeTime < leastUsed.lastFreeTime) {
                        leastUsed = freeServers.get(i);
                    }
                }
                return leastUsed; // Pick the server that has been free the longest
            }
        }
        return null;
    }

    // Handles status updates from a registered server
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
