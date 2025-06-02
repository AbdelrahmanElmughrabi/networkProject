package newPackageoOop;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client2 {

    private int choice;
    private String type;

    public Client2(int choice, String type) {
        this.choice = choice;
        this.type = type;
    }

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
                System.out.println("From Server: " + line);
            }
        }
        return response.toString();
    }

    private int getPort(int choice) throws IOException {
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
        Client2 client = new Client2(choice, type);
        String response = client.runRequest();
        System.out.println("From Server: " + response);
    }
}
