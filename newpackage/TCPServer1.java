package newpackage;

import java.io.*;
import java.net.*;

public class TCPServer1 {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String strategy = args.length > 1 ? args[1] : "static";
        Socket lb = new Socket("localhost", 6789);
        DataOutputStream outLb = new DataOutputStream(lb.getOutputStream());
        outLb.writeBytes("JOIN " + port + " " + strategy + "\n");
        outLb.flush();
        BufferedReader inLb = new BufferedReader(new InputStreamReader(lb.getInputStream()));
        if (!inLb.readLine().equals("OK")) {
            return;
        }
        ServerSocket ss = new ServerSocket(port);
        while (true) {
            Socket c = ss.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
            DataOutputStream out = new DataOutputStream(c.getOutputStream());
            int choice = Integer.parseInt(in.readLine());
            String type = in.readLine();
            out.writeBytes(handle(choice, type));
            out.flush();
            DataOutputStream outLb2 = new DataOutputStream(lb.getOutputStream());
            outLb2.writeBytes("FREE\n");
            outLb2.flush();
            c.close();
        }
    }

    static String handle(int choice, String type) {
        try {
            if (choice == 1) {
                File d = new File(type);
                if (d.isDirectory()) {
                    StringBuilder sb = new StringBuilder();
                    for (File f : d.listFiles()) {
                        sb.append(f.getName()).append("\n");
                    }
                    return sb.toString();
                }
                return "Invalid dir\n";
            } else if (choice == 2) {
                File f = new File(type);
                if (f.isFile()) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader r = new BufferedReader(new FileReader(f));
                    String l;
                    while ((l = r.readLine()) != null) {
                        sb.append(l).append("\n");
                    }
                    r.close();
                    return sb.toString();
                }
                return "No file\n";
            } else if (choice == 3) {
                int t = Integer.parseInt(type);
                Thread.sleep(t * 1000);
                return "Done\n";
            } else if (choice == 4) {
                int t = Integer.parseInt(type);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < t; i++) {
                    sb.append("Frame ").append(i).append("\n");
                    Thread.sleep(1000);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "Err\n";
        }
        return "Bad\n";
    }
}
