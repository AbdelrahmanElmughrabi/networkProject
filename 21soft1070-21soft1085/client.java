
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

class client {

    public static void main(String[] args) throws IOException {
        Scanner scn = new Scanner(System.in);
        System.out.println("choose your request type \n1.Directory listing\n2.File transfer \n3.Computation \n4. Video streaming");
        int choice = scn.nextInt();
        System.out.println("Enter your specific request:");
        String type = scn.next();
        communicateWithServer(choice, type);
    }

    public static void communicateWithServer(int choice, String type) throws IOException {
        // First get server port from load balancer
        int serverPort = getServerPort(choice); //getting the server port that serve the choice from the load balancer

        // Connect to the assigned server
        Socket serverSocket = new Socket("localhost", serverPort);
        DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

        // Send both choice and type to server
        outToServer.writeBytes(choice + "\n");
        outToServer.writeBytes(type + "\n");

        // Receive response from server
        String response = inFromServer.readLine();
        System.out.println("From Server: " + response);

        serverSocket.close();
    }

    private static int getServerPort(int choice) throws IOException {
        Socket lbSocket = new Socket("localhost", 6789);
        DataOutputStream outToLb = new DataOutputStream(lbSocket.getOutputStream());
        BufferedReader inFromLb = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));

        outToLb.writeBytes(choice + "\n");
        String port = inFromLb.readLine();
        lbSocket.close();

        return Integer.parseInt(port);
    }


    public static void pdfFile(Socket serverSocekt){
        
    }


//Ideas: each request have a number to identify them
// Or and to have the ability to send files on multiple chunks













}
