package newpackage;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client1 {

    // Main method: supports both CLI and interactive input
    public static void main(String[] args) throws IOException {
        int choice;
        String type;
        if (args.length >= 2) {
            choice = Integer.parseInt(args[0]);
            type = args[1];
        } else {
            Scanner sc = new Scanner(System.in);
            System.out.println("Choose your request type:\n1. Directory listing\n2. File transfer\n3. Computation\n4. Video streaming");
            choice = sc.nextInt();
            sc.nextLine();
            System.out.println("Enter your specific request:");
            type = sc.nextLine();
        }
        String response = runRequest(choice, type);
        System.out.println("From Server: " + response);
    }

    // Sends a request to the server and returns the response as a string
    public static String runRequest(int choice, String type) throws IOException {
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
                System.out.println("From Server: " + line);
            }
        }
        return response.toString();
    }

    // Gets the port of an available server from the load balancer
    private static int getPort(int choice) throws IOException {
        try (Socket lb = new Socket("localhost", 6789)) {
            DataOutputStream out = new DataOutputStream(lb.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(lb.getInputStream()));
            out.writeBytes("REQUEST " + choice + "\n");
            out.flush();
            String p = in.readLine();
            if (p.equals("NO_SERVER")) {
                throw new IOException("No server available");
            }
            return Integer.parseInt(p);
        }
    }
}
