package newpackage;

import java.io.*;
import java.nio.file.Files;

public class main1 {

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
                LoadBalancer1.main(new String[]{"static"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        lbThread.start();
        
        Server1.startTwoServers();

        // Step 4: Wait for servers to register
        System.out.println("Waiting for servers to register...");
        Thread.sleep(5000);

        // Step 5: Test all request types
        System.out.println("\n=== Testing Request Types ===");
        try {
            System.out.println("Directory listing response: " + Client1.runRequest(1, dirPath));
        } catch (IOException e) {
            System.out.println("Directory listing error: " + e.getMessage());
        }
        Thread.sleep(1000);
        try {
            System.out.println("File transfer response: " + Client1.runRequest(2, filePath));
        } catch (IOException e) {
            System.out.println("File transfer error: " + e.getMessage());
        }
        Thread.sleep(1000);
        try {
            System.out.println("Computation response: " + Client1.runRequest(3, "2"));
        } catch (IOException e) {
            System.out.println("Computation error: " + e.getMessage());
        }
        Thread.sleep(1000);
        try {
            System.out.println("Video streaming response: " + Client1.runRequest(4, "2"));
        } catch (IOException e) {
            System.out.println("Video streaming error: " + e.getMessage());
        }
        Thread.sleep(1000);

        // Step 6: Demonstrate load balancing with concurrent requests
        System.out.println("\n=== Testing Load Balancing ===");
        Thread client1 = new Thread(() -> {
            try {
                System.out.println("First concurrent response: " + Client1.runRequest(1, dirPath));
            } catch (IOException e) {
                System.out.println("First concurrent error: " + e.getMessage());
            }
        });
        Thread client2 = new Thread(() -> {
            try {
                System.out.println("Second concurrent response: " + Client1.runRequest(1, dirPath));
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
            System.out.println("Invalid directory response: " + Client1.runRequest(1, "/non/existent/path"));
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
                LoadBalancer1.main(new String[]{"dynamic"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        newLbThread.start();
        Thread.sleep(5000);
        try {
            System.out.println("Dynamic strategy response: " + Client1.runRequest(1, dirPath));
        } catch (IOException e) {
            System.out.println("Dynamic strategy error: " + e.getMessage());
        }

        // Step 9: Clean up temporary files
        System.out.println("\n=== Cleaning Up ===");
        if (tempFile.exists()) {
            tempFile.delete();
        }
        if (tempDir.exists()) {
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
