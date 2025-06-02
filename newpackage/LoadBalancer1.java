package newpackage;

import java.io.*;
import java.net.*;
import java.util.*;

public class LoadBalancer1 {

    static List<ServerInfo> servers = new ArrayList<>();
    static String strategy = "static";

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java LoadBalancer1 <strategy>");
            return;
        }
        strategy = args[0];
        ServerSocket ss = new ServerSocket(6789);
        System.out.println("Load Balancer listening on port 6789 with strategy " + strategy);
        while (true) {
            Socket s = ss.accept();
            new Thread(() -> handle(s)).start();
        }
    }

    // Handles incoming connections (from servers or clients)
    static void handle(Socket s) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            String msg = in.readLine();
            if (msg == null) {
                s.close();
                return;
            }
            if (msg.startsWith("JOIN")) {
                String[] parts = msg.split(" ");
                int port = Integer.parseInt(parts[1]);
                String strat = parts[2];

                ServerInfo info = new ServerInfo(port, strat, false, s);
                synchronized (servers) {
                    servers.add(info);
                }
                out.writeBytes("OK\n");
                out.flush();
                new Thread(() -> serverStatus(info)).start();
            } else if (msg.startsWith("REQUEST")) {
                ServerInfo selected = selectServer();
                if (selected != null) {
                    selected.busy = true;
                    out.writeBytes(selected.port + "\n");
                } else {
                    out.writeBytes("NO_SERVER\n");
                }
                out.flush();
                s.close();
            } else {
                s.close();
            }
        } catch (Exception e) {
            // Optionally print error
        }
    }

    // Selects a server based on the current load balancing strategy
    static ServerInfo selectServer() {
        synchronized (servers) {
            List<ServerInfo> free = new ArrayList<>();
            for (ServerInfo srv : servers) {
                if (!srv.busy) {
                    free.add(srv);
                }
            }
            if (free.isEmpty()) {
                return null;
            }
            if (strategy.equals("static")) {
                return free.get(0);
            } else { // dynamic
                ServerInfo leastUsed = free.get(0);
                for (ServerInfo s : free) {
                    if (s.lastFreeTime < leastUsed.lastFreeTime) {
                        leastUsed = s;
                    }
                }
                return leastUsed;
            }
        }
    }

    // Handles status updates from a registered server
    static void serverStatus(ServerInfo srv) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(srv.sock.getInputStream()));
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equals("FREE")) {
                    srv.busy = false;
                    srv.lastFreeTime = System.currentTimeMillis();
                } else if (msg.equals("GOODBYE")) {
                    synchronized (servers) {
                        servers.remove(srv);
                    }
                    srv.sock.close();
                    break;
                }
            }
        } catch (Exception e) {
            // Remove server if connection is lost
            synchronized (servers) {
                servers.remove(srv);
            }
        }
    }

    // Simple server metadata class
    static class ServerInfo {

        int port;
        String strategy;
        boolean busy;
        Socket sock;
        long lastFreeTime;

        ServerInfo(int p, String s, boolean b, Socket sk) {
            port = p;
            strategy = s;
            busy = b;
            sock = sk;
            lastFreeTime = System.currentTimeMillis();
        }
    }
}
