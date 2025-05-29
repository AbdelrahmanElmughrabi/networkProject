// Server.java
// Represents a server registered with the load balancer

import java.net.Socket; // For the server's socket connection

public class Server {

    int port;           // The port number the server listens on
    String strategy;    // The load balancing strategy used by the server
    boolean isBusy;     // Whether the server is currently handling a request
    Socket socket;      // The socket connection to the load balancer
    long lastFreeTime;  // The last time the server was marked as free

    // Constructor to initialize a Server object
    public Server(int port, String strategy, boolean isBusy, Socket socket) {
        this.port = port;                   // Set the server's port
        this.strategy = strategy;           // Set the server's strategy
        this.isBusy = isBusy;               // Set the server's busy status
        this.socket = socket;               // Set the server's socket
        this.lastFreeTime = System.currentTimeMillis(); // Initialize last free time to now
    }
}
