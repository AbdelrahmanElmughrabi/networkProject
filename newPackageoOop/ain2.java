package newPackageoOop;

import java.io.*;
import java.nio.file.Files;

public class ain2 {

    public static void main(String[] args) throws Exception {
        // Step 1: Create temporary directory and file
        File tempDir = Files.createTempDirectory("testDir").toFile();
        File tempFile = new File(tempDir, "test.txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("Test content\n");
        }
        String dirPath = tempDir.getAbsolutePath();
        String filePath = tempFile.getAbsolutePath();

        // Step 2: Start the load balancer (static strategy)
        System.out.println("Starting load balancer with static strategy on port 6789...");
        Thread lbThread = new Thread(() -> {
            try {
                LoadBalancer2.main(new String[]{"static"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        lbThread.start();

        // Start servers
        Server2.startTwoServers();

        // Step 4: Wait for servers to register
        System.out.println("Waiting for servers to register...");
        Thread.sleep(5000);

        // Step 5: Test all request types
        System.out.println("\n=== Testing Request Types ===");
        try {
            Client2 client = new Client2(1, dirPath);
            System.out.println("Directory listing response: " + client.runRequest());
        } catch (IOException e) {
            System.out.println("Directory listing error: " + e.getMessage());
        }
        Thread.sleep(1000);
        try {
            Client2 client = new Client2(2, filePath);
            System.out.println("File transfer response: " + client.runRequest());
        } catch (IOException e) {
            System.out.println("File transfer error: " + e.getMessage());
        }
        Thread.sleep(1000);
        try {
            Client2 client = new Client2(3, "2");
            System.out.println("Computation response: " + client.runRequest());
        } catch (IOException e) {
            System.out.println("Computation error: " + e.getMessage());
        }
        Thread.sleep(1000);
        try {
            Client2 client = new Client2(4, "2");
            System.out.println("Video streaming response: " + client.runRequest());
        } catch (IOException e) {
            System.out.println("Video streaming error: " + e.getMessage());
        }
        Thread.sleep(1000);

        // Step 6: Demonstrate load balancing with concurrent requests
        System.out.println("\n=== Testing Load Balancing ===");
        Thread client1 = new Thread(() -> {
            try {
                Client2 c = new Client2(1, dirPath);
                System.out.println("First concurrent response: " + c.runRequest());
            } catch (IOException e) {
                System.out.println("First concurrent error: " + e.getMessage());
            }
        });
        Thread client2 = new Thread(() -> {
            try {
                Client2 c = new Client2(1, dirPath);
                System.out.println("Second concurrent response: " + c.runRequest());
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
            Client2 client = new Client2(1, "/non/existent/path");
            System.out.println("Invalid directory response: " + client.runRequest());
        } catch (IOException e) {
            System.out.println("Invalid directory error: " + e.getMessage());
        }

        // Step 8: Test dynamic strategy
        System.out.println("\n=== Testing Dynamic Load Balancing Strategy ===");
        System.out.println("Restarting load balancer with dynamic strategy...");
        lbThread.interrupt();
        try {
            lbThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Thread newLbThread = new Thread(() -> {
            try {
                LoadBalancer2.main(new String[]{"dynamic"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        newLbThread.start();
        Thread.sleep(5000);
        try {
            Client2 client = new Client2(1, dirPath);
            System.out.println("Dynamic strategy response: " + client.runRequest());
        } catch (IOException e) {
            System.out.println("Dynamic strategy error: " + e.getMessage());
        }

        // Step 9: Clean up temporary files
        System.out.println("\n=== Cleaning Up ===");
        if (tempFile.exists()) {
            tempFile.delete();
        }
        if (tempDir.exists() && tempDir.listFiles().length == 0) {
            tempDir.delete();
        }

        // Step 10: Keep system running for observation
        System.out.println("\n=== System Running ===");
        System.out.println("Press Ctrl+C to stop the demonstration.");
        while (true) {
            Thread.sleep(1000);
        }
    }
}
