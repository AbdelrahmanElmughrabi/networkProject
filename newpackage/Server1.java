package newpackage;

import java.io.*;
import java.net.*;

public class Server1 {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java Server1 <port> <strategy>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        String strategy = args[1];

        // Register with the load balancer
        Socket lbSocket = new Socket("localhost", 6789);
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

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port + " with strategy " + strategy);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket, lbSocket)).start();
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

    // Handles a single client connection
    private static void handleClient(Socket clientSocket, Socket lbSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            String choiceStr = in.readLine();
            String type = in.readLine();
            if (choiceStr == null || type == null) {
                out.writeBytes("Invalid request\n");
                out.flush();
                return;
            }
            int choice = Integer.parseInt(choiceStr);
            String result = handle(choice, type);
            out.writeBytes(result + "\n");
            out.flush();
            // Notify load balancer this server is free
            DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
            outToLb.writeBytes("FREE\n");
            outToLb.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Processes the client's request
    private static String handle(int choice, String type) {
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
    public static void startTwoServers() {
        int port = 7000;
        String[] Strategy = {"static", "dynamic"};

        System.out.println("Starting server 1 on port 7000 with static strategy...");
        for (int i = 0; i < 2; i++) {
            String currentStrategy;
            if (port % 2 == 0) {
                currentStrategy = Strategy[0];
            } else {
                currentStrategy = Strategy[1];
            }
            String portStr = String.valueOf(port);
            Thread serverThread = new Thread(() -> {
                try {
                    // Increment port for each server
                    Server1.main(new String[]{portStr, currentStrategy});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
            port++;
        }
    }
}
