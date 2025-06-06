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
    private static final int LB_PORT = 6789; // Unified port

    public Server2(int port, String strategy) {
        this(port, strategy, LB_PORT);
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

    // Combined and simplified client handler
    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            String choiceStr = in.readLine();
            String type = in.readLine();

            if (choiceStr == null || type == null) {
                out.writeBytes("Invalid request\n");
                out.flush();
                System.out.println("Received invalid request from client.");
                return;
            }

            int choice;
            try {
                choice = Integer.parseInt(choiceStr);
            } catch (NumberFormatException e) {
                out.writeBytes("Invalid choice format\n");
                out.flush();
                System.out.println("Invalid choice format from client.");
                return;
            }

            String response = null;

            switch (choice) {
                case 1: // Directory listing
                    File dir = new File(type);
                    if (dir.isDirectory()) {
                        StringBuilder sb = new StringBuilder();
                        File[] files = dir.listFiles();
                        if (files != null) {
                            for (File f : files) {
                                sb.append(f.getName()).append("\n");
                            }
                        }
                        response = sb.toString();
                    } else {
                        response = "Invalid directory";
                    }
                    break;

                case 2: // File transfer
                    File file = new File(type);
                    if (file.isFile()) {
                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                            String line;
                            while ((line = r.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                        } catch (IOException e) {
                            response = "Error reading file: " + e.getMessage();
                            break;
                        }
                        response = sb.toString();
                    } else {
                        response = "File not found";
                    }
                    break;

                case 3: // Computation
                    try {
                        int t = Integer.parseInt(type);
                        Thread.sleep(t * 1000L);
                        response = "Computation done";
                    } catch (NumberFormatException e) {
                        response = "Invalid computation time";
                    }
                    break;

                case 4: // Video streaming
                    try {
                        int frames = Integer.parseInt(type);
                        for (int i = 0; i < frames; i++) {
                            out.writeBytes("Video frame " + i + "\n");
                            out.flush();
                            System.out.println("Sent video frame " + i + " to client " + clientSocket.getRemoteSocketAddress());
                            Thread.sleep(1000);
                        }
                    } catch (NumberFormatException e) {
                        out.writeBytes("Invalid frame count\n");
                        out.flush();
                    }
                    response = null; // Already sent frames
                    break;

                default:
                    response = "Invalid choice";
            }

            if (response != null) {
                out.writeBytes(response + "\n");
                out.flush();
            }

            System.out.println("Processed request (choice=" + choiceStr + ", type=" + type + ") for client " + clientSocket.getRemoteSocketAddress());

            // Notify load balancer this server is free
            try {
                DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
                outToLb.writeBytes("FREE\n");
                outToLb.flush();
                System.out.println("Notified load balancer that server is free.");
            } catch (IOException e) {
                System.out.println("Failed to notify load balancer: " + e.getMessage());
            }
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

    // Starts two server instances on different ports and strategies
    public static void startTwoServers(int serverNum) {
        int port = 7000;
        String[] Strategy = {"static", "dynamic"};

        System.out.println("Starting servers with static and dynamic strategies...");
        for (int i = 0; i < serverNum; i++) {
            String currentStrategy = (port % 2 == 0) ? Strategy[0] : Strategy[1];
            int currentPort = port;
            Thread serverThread = new Thread(() -> {
                try {
                    new Server2(currentPort, currentStrategy, LB_PORT).start();
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
        System.out.println("Enter the number of servers you want to start (1 or 2): ");
        int serverNum = scn.nextInt();
        startTwoServers(serverNum);

        if (args.length >= 2) {
            int port = Integer.parseInt(args[0]);
            String strategy = args[1];
            new Server2(port, strategy, LB_PORT).start();
        }
    }

}
