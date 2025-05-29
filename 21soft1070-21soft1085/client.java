
import java.io.*; // For input/output streams
import java.net.*; // For networking (Socket)
import java.util.*; // For Scanner

// Client Class
public class Client {

    // Main method: Entry point for the client application
    public static void main(String[] args) throws IOException {
        int choice; // To store the user's request type
        String type; // To store the user's specific request (directory, file, etc.)
        if (args.length >= 2) { // If arguments are provided via command line
            choice = Integer.parseInt(args[0]); // Parse the request type
            type = args[1]; // Get the specific request
        } else { // If no command line arguments, prompt user for input
            Scanner scn = new Scanner(System.in); // Scanner for user input
            System.out.println("Choose your request type:\n1. Directory listing\n2. File transfer\n3. Computation\n4. Video streaming");
            choice = scn.nextInt(); // Read the request type
            scn.nextLine(); // Consume the newline character
            System.out.println("Enter your specific request:");
            type = scn.nextLine(); // Read the specific request
        }
        String response = runRequest(choice, type); // Send the request and get the response
        System.out.println("From Server: " + response); // Print the server's response
    }

    // Sends a request to the server and returns the response as a string
    public static String runRequest(int choice, String type) throws IOException {
        StringBuilder response = new StringBuilder(); // To accumulate the server's response
        int serverPort = getServerPort(choice); // Get the server port from the load balancer

        // Connect to the assigned server
        try (Socket serverSocket = new Socket("localhost", serverPort)) {
            DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream()); // For sending data to server
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream())); // For reading server response

            outToServer.writeBytes(choice + "\n"); // Send the request type
            outToServer.writeBytes(type + "\n"); // Send the specific request
            outToServer.flush(); // Ensure data is sent

            String line;
            // Read each line of the server's response
            while ((line = inFromServer.readLine()) != null) {
                response.append(line).append("\n"); // Append to response
                System.out.println("From Server: " + line); // Print each line as received
            }
        }
        return response.toString(); // Return the full response
    }

    // Connects to the load balancer to get an available server port for the request
    private static int getServerPort(int choice) throws IOException {
        // Connect to the load balancer on port 6789
        try (Socket lbSocket = new Socket("localhost", 6789)) {
            DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream()); // For sending data to load balancer
            BufferedReader inFromLb = new BufferedReader(new InputStreamReader(lbSocket.getInputStream())); // For reading load balancer response

            outToLb.writeBytes("REQUEST " + choice + "\n"); // Send the request type to load balancer
            outToLb.flush(); // Ensure data is sent
            String portStr = inFromLb.readLine(); // Read the assigned server port
            if (portStr.equals("NO_SERVER")) { // If no server is available
                throw new IOException("No server available"); // Throw an exception
            }
            int serverPort = Integer.parseInt(portStr); // Parse the server port
            System.out.println("Assigned to server port: " + serverPort); // Print the assigned port
            return serverPort; // Return the port
        }
    }
}
