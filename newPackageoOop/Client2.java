package newPackageoOop;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Client2 is a reusable client for sending requests to the load balancer and
 * servers.
 */
public class Client2 {

    private static final int DEFAULT_STATIC_LB_PORT = 6789;
    private static final int DEFAULT_DYNAMIC_LB_PORT = 6790;

    private int choice;
    private String type;
    private int lbPort;

    /**
     * Constructs a Client2 with the given request type and value, using the
     * default static LB port.
     */
    public Client2(int choice, String type) {
        this(choice, type, DEFAULT_STATIC_LB_PORT);
    }

    /**
     * Constructs a Client2 with the given request type, value, and load
     * balancer port.
     */
    public Client2(int choice, String type, int lbPort) {
        this.choice = choice;
        this.type = type;
        this.lbPort = lbPort;
    }

    /**
     * Sends the request to the server via the load balancer and returns the
     * response.
     */
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
        return "Video streaming request completed successfully.";
    }

    private int getPort(int choice) throws IOException {
        try (Socket lb = new Socket("localhost", lbPort)) {
            DataOutputStream out = new DataOutputStream(lb.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(lb.getInputStream()));
            out.writeBytes("REQUEST " + choice + "\n");
            out.flush();
            String p = in.readLine();
            if ("NO_SERVER".equals(p)) {
                throw new IOException("No server available");
            }
            return Integer.parseInt(p);
        }
    }

    /**
     * Returns the recommended default LB port for a given request type.
     */
    public static int getDefaultLbPortForChoice(int choice) {
        switch (choice) {
            case 1: // Directory listing
            case 2: // File transfer
                return DEFAULT_STATIC_LB_PORT;
            case 3: // Computation
            case 4: // Video streaming
                return DEFAULT_DYNAMIC_LB_PORT;
            default:
                return DEFAULT_STATIC_LB_PORT;
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
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
            }

            int lbPort = getDefaultLbPortForChoice(choice);
            Client2 client = new Client2(choice, type, lbPort);

            String response = client.runRequest();
            System.out.println(response);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
