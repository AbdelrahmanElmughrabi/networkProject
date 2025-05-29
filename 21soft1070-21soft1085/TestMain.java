
import java.io.*;
import java.nio.file.Files;

// TestMain Class to Orchestrate Testing
public class TestMain {

    public static void main(String[] args) throws Exception {
        // Step 1: Create temporary directory and file
        File tempDir = Files.createTempDirectory("testDir").toFile();
        File tempFile = new File(tempDir, "test.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Test content\n");
        }
        String dirPath = tempDir.getAbsolutePath();
        String filePath = tempFile.getAbsolutePath();

        // Step 2: Start the load balancer
        System.out.println("Starting load balancer with static strategy on port 6789...");
        Thread lbThread = new Thread(() -> {
            try {
                LoadBalancer.main(new String[]{"static"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        lbThread.start();

        // Step 3: Start two servers
        System.out.println("Starting server 1 on port 7000 with static strategy...");
        Thread server1Thread = new Thread(() -> {
            try {
                TCPServer.main(new String[]{"7000", "static"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server1Thread.start();

        System.out.println("Starting server 2 on port 7001 with dynamic strategy...");
        Thread server2Thread = new Thread(() -> {
            try {
                TCPServer.main(new String[]{"7001", "dynamic"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server2Thread.start();

        // Step 4: Wait for servers to register
        System.out.println("Waiting for servers to register...");
        Thread.sleep(5000);

        // Step 5: Test all request types
        System.out.println("\n=== Testing Request Types ===");

        // Directory Listing
        System.out.println("Sending directory listing request...");
        try {
            String dirResponse = Client.runRequest(1, dirPath);
            System.out.println("Directory listing response: " + dirResponse);
        } catch (IOException e) {
            System.out.println("Directory listing error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // File Transfer
        System.out.println("Sending file transfer request...");
        try {
            String fileResponse = Client.runRequest(2, filePath);
            System.out.println("File transfer response: " + fileResponse);
        } catch (IOException e) {
            System.out.println("File transfer error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // Computation
        System.out.println("Sending computation request for 5 seconds...");
        try {
            String compResponse = Client.runRequest(3, "5");
            System.out.println("Computation response: " + compResponse);
        } catch (IOException e) {
            System.out.println("Computation error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // Video Streaming
        System.out.println("Sending video streaming request for 3 seconds...");
        try {
            String streamResponse = Client.runRequest(4, "3");
            System.out.println("Video streaming response: " + streamResponse);
        } catch (IOException e) {
            System.out.println("Video streaming error: " + e.getMessage());
        }
        Thread.sleep(1000); // Wait for server to send FREE

        // Step 6: Demonstrate load balancing with concurrent requests
        System.out.println("\n=== Testing Load Balancing ===");
        System.out.println("Sending two concurrent directory listing requests...");
        Thread client1 = new Thread(() -> {
            try {
                String resp1 = Client.runRequest(1, dirPath);
                System.out.println("First concurrent response: " + resp1);
            } catch (IOException e) {
                System.out.println("First concurrent error: " + e.getMessage());
            }
        });
        Thread client2 = new Thread(() -> {
            try {
                String resp2 = Client.runRequest(1, dirPath);
                System.out.println("Second concurrent response: " + resp2);
            } catch (IOException e) {
                System.out.println("Second concurrent error: " + e.getMessage());
            }
        });
        client1.start();
        client2.start();
        client1.join();
        client2.join();

        // Step 7: Test edge case - invalid directory
        System.out.println("\n=== Testing Edge Case: Invalid Directory ===");
        try {
            String invalidResponse = Client.runRequest(1, "/non/existent/path");
            System.out.println("Invalid directory response: " + invalidResponse);
        } catch (IOException e) {
            System.out.println("Invalid directory error: " + e.getMessage());
        }

        // Step 8: Test dynamic strategy
        System.out.println("\n=== Testing Dynamic Load Balancing Strategy ===");
        System.out.println("Restarting load balancer with dynamic strategy...");
        lbThread.interrupt();
        try {
            lbThread.join(); // Wait for the old load balancer to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Old load balancer stopped.");

        // Start new load balancer with dynamic strategy
        Thread newLbThread = new Thread(() -> {
            try {
                LoadBalancer.main(new String[]{"dynamic"});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        newLbThread.start();
        lbThread = newLbThread; // Update reference

        // Wait for servers to re-register
        Thread.sleep(5000);
        System.out.println("Sending directory listing request with dynamic strategy...");
        try {
            String dynamicResponse = Client.runRequest(1, dirPath);
            System.out.println("Dynamic strategy response: " + dynamicResponse);
        } catch (IOException e) {
            System.out.println("Dynamic strategy error: " + e.getMessage());
        }

        // Step 9: Note on dynamic server management
        System.out.println("\n=== Dynamic Server Management ===");
        System.out.println("Note: Server deregistration via 'GOODBYE' is supported. To test, modify TCPServer to send 'GOODBYE' after a request.");

        // Step 10: Clean up temporary files
        System.out.println("\n=== Cleaning Up ===");
        if (tempFile.exists()) {
            tempFile.delete();
        }
        if (tempDir.exists()) {
            tempDir.delete();
        }

        // Step 11: Keep system running for observation
        System.out.println("\n=== System Running ===");
        System.out.println("Press Ctrl+C to stop the demonstration.");
        while (true) {
            Thread.sleep(1000);
        }
    }
}
