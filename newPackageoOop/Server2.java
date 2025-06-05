package newPackageoOop;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server2 {

    private int port;
    private String strategy;
    private Socket lbSocket;
    private ServerSocket serverSocket;
    private int lbPort; // Add this field

    public Server2(int port, String strategy) {
        this(port, strategy, strategy.equals("static") ? 6789 : 6790); // Default LB port by strategy
    }

    public Server2(int port, String strategy, int lbPort) {
        this.port = port;
        this.strategy = strategy;
        this.lbPort = lbPort;
    }

    public void start() throws IOException {
        // Register with the load balancer
        lbSocket = new Socket("localhost", lbPort);
        DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
        outToLb.writeBytes("JOIN " + port + " " + strategy + "\n");
        outToLb.flush();
        BufferedReader inFromLb = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
        String response = inFromLb.readLine();
        if (!"OK".equals(response)) {
            System.out.println("Failed to join load balancer");
            lbSocket.close();
            return;
        }
        System.out.println("Successfully registered with load balancer on port " + lbPort);

        serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port + " with strategy " + strategy);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from client: " + clientSocket.getRemoteSocketAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (SocketException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                e.printStackTrace();
            }
        }
        serverSocket.close();
        lbSocket.close();
        System.out.println("Server on port " + port + " shutting down.");
    }

    // Handles a single client connection
    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            String choiceStr = in.readLine();
            String type = in.readLine();
            if (choiceStr == null || type == null) {
                out.writeBytes("Invalid request\n");
                out.flush();
                System.out.println("Received invalid request from client.");
                return;
            }
            int choice = Integer.parseInt(choiceStr);
            String result = handle(choice, type);
            out.writeBytes(result + "\n");
            out.flush();
            System.out.println("Processed request (choice=" + choice + ", type=" + type + ") for client " + clientSocket.getRemoteSocketAddress());
            // Notify load balancer this server is free
            DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
            outToLb.writeBytes("FREE\n");
            outToLb.flush();
            System.out.println("Notified load balancer that server is free.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Closed connection with client.");
            } catch (IOException ignored) {
            }
        }
    }

    // Processes the client's request
    private String handle(int choice, String type) {
        try {
            switch (choice) {
                case 1: // Directory listing
                    File d = new File(type);
                    if (d.isDirectory()) {
                        StringBuilder sb = new StringBuilder();
                        for (File f : d.listFiles()) {
                            sb.append(f.getName()).append("\n");
                        }
                        return sb.toString();
                    }
                    return "Invalid directory";

                case 2: // File transfer
                    File f = new File(type);
                    if (f.isFile()) {
                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                            String l;
                            while ((l = r.readLine()) != null) {
                                sb.append(l).append("\n");
                            }
                        }
                        return sb.toString();
                    }
                    return "File not found";

                case 3: // Computation
                    int t = Integer.parseInt(type);
                    Thread.sleep(t * 1000);
                    return "Computation done";

                case 4: // Video streaming
                    int frames = Integer.parseInt(type);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < frames; i++) {
                        sb.append("Video frame ").append(i).append("\n");
                        Thread.sleep(1000);
                    }
                    return sb.toString();

                default:
                    return "Invalid choice";
            }
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    // Starts two server instances on different ports and strategies
    public static void startTwoServers(int serverNum) {
        int port = 7000;
        String[] Strategy = {"static", "dynamic"};
        int[] lbPorts = {6789, 6790};

        System.out.println("Starting servers with static and dynamic strategies...");
        for (int i = 0; i < serverNum; i++) {
            String currentStrategy = (port % 2 == 0) ? Strategy[0] : Strategy[1];
            int currentLbPort = (port % 2 == 0) ? lbPorts[0] : lbPorts[1];
            int currentPort = port;
            Thread serverThread = new Thread(() -> {
                try {
                    new Server2(currentPort, currentStrategy, currentLbPort).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
            port++;
        }
    }

    public static void main(String[] args) throws IOException {

        Scanner scn = new Scanner(System.in);
        System.out.println("Enter the numebr of server you want to start (1 or 2): ");
        int serverNum = scn.nextInt();
        startTwoServers(serverNum);

        int port = Integer.parseInt(args[0]);
        String strategy = args[1];
        new Server2(port, strategy).start();
    }

}
