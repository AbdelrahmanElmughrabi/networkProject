package networkProject.newPackage2;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final int LB_PORT = 6789;
    private int choice;
    private String type;
    private int lbPort;

    // Constructs a client with the given request type and value, using the default LB port
    public Client(int choice, String type) {
        this(choice, type, LB_PORT);
    }

    // Constructs a client with the given request type, value, and load balancer port
    public Client(int choice, String type, int lbPort) {
        this.choice = choice;
        this.type = type;
        this.lbPort = lbPort;
    }

    // Sends the request to the server via the load balancer and returns the response
    public String runRequest() throws IOException {
        StringBuilder response = new StringBuilder();
        int port = getPort(choice);
        try (Socket s = new Socket("localhost", port)) {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out.writeBytes(choice + "\n");
            out.writeBytes(type + "\n");
            out.flush();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                System.out.println("Received: " + line);
            }
        }
        return getChoiceName(choice) + " request completed successfully.";
    }

    // Requests a server port from the load balancer based on the request type
    private int getPort(int choice) throws IOException {
        try (Socket lb = new Socket("localhost", lbPort)) {
            DataOutputStream out = new DataOutputStream(lb.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(lb.getInputStream()));
            String requestType;
            switch (choice) {
                case 1:
                    requestType = "file";
                    break;
                case 2:
                    requestType = "file";
                    break;
                case 3:
                    requestType = "compute";
                    break;
                case 4:
                    requestType = "stream";
                    break;
                default:
                    requestType = "file";
            }
            out.writeBytes("REQUEST " + requestType + "\n");
            out.flush();
            String p = in.readLine();
            if ("NO_SERVER".equals(p)) {
                throw new IOException("No server available");
            }
            return Integer.parseInt(p);
        }
    }

    // Runs multiple clients in parallel for testing
    public static void testHundredClients(int numClients) {
        Thread[] threads = new Thread[numClients];
        for (int i = 0; i < numClients; i++) {
            final int clientNum = i % 4 + 1;
            final String type = (clientNum == 1) ? "testDir"
                    : (clientNum == 2) ? "testFile.txt"
                            : (clientNum == 3) ? "5"
                                    : "10";
            threads[i] = new Thread(() -> {
                try {
                    Client client = new Client(clientNum, type, LB_PORT);
                    String response = client.runRequest();
                    System.out.println("Client finished: " + response);
                } catch (Exception e) {
                    System.out.println("Client error: " + e.getMessage());
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }
        System.out.println("All clients finished.");
    }

    // Returns a descriptive name for the request type
    private String getChoiceName(int choice) {
        switch (choice) {
            case 1:
                return "Directory listing";
            case 2:
                return "File transfer";
            case 3:
                return "Computation";
            case 4:
                return "Video streaming";
            default:
                return "Unknown";
        }
    }

    // Entry point for running a single client or multiple clients for testing
    public static void main(String[] args) {
        int x = 0;
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter 0 to choose an action or 1 to create number of clients:");
            x = scanner.nextInt();

            if (x == 0) {
                System.out.println("Enter request type number:");
                System.out.println("1 = Directory listing");
                System.out.println("2 = File transfer");
                System.out.println("3 = Computation");
                System.out.println("4 = Video streaming");
                System.out.print("Choice: ");
                int choice = Integer.parseInt(scanner.nextLine().trim());
                String type = null;
                if (choice == 1) {
                    System.out.println("Enter directory path:");
                    type = scanner.nextLine().trim();
                } else if (choice == 2) {
                    System.out.println("Enter file path:");
                    type = scanner.nextLine().trim();
                    System.out.println("Sending the file");
                } else if (choice == 3) {
                    System.out.println("Enter number of seconds:");
                    type = scanner.nextLine().trim();
                } else if (choice == 4) {
                    System.out.println("Enter number of frames:");
                    type = scanner.nextLine().trim();
                } else {
                    System.out.println("Invalid choice");
                    return;
                }
                Client client = new Client(choice, type, LB_PORT);
                String response = client.runRequest();
                System.out.println(response);
            } else {
                System.out.println("Enter number of clients to create (e.g., 100):");
                int numClients = scanner.nextInt();
                testHundredClients(numClients);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
