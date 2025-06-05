package newPackageoOop;

import java.io.*;
import java.net.*;
import java.util.*;

public class LoadBalancer2 {

    private List<ServerInfo> servers = new ArrayList<>();
    private String strategy = "static";
    private int port = 6789;

    public LoadBalancer2(String strategy, int port) {
        this.strategy = strategy;
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Load Balancer listening on port " + port + " with strategy " + strategy);
        while (true) {
            Socket s = ss.accept();
            System.out.println("Accepted connection from: " + s.getRemoteSocketAddress());
            new Thread(() -> handle(s)).start();
        }
    }

    // Handles incoming connections (from servers or clients)
    private void handle(Socket s) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            String msg = in.readLine();
            if (msg == null) {
                s.close();
                return;
            }
            //Server Registration
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
                System.out.println("Registered server on port " + port + " with strategy " + strat);
                new Thread(() -> serverStatus(info)).start();

                //Client Request Handling
            } else if (msg.startsWith("REQUEST")) {
                ServerInfo selected = selectServer();
                if (selected != null) {
                    selected.busy = true;
                    out.writeBytes(selected.port + "\n");
                    System.out.println("Assigned client to server on port " + selected.port);
                } else {
                    out.writeBytes("NO_SERVER\n");
                    System.out.println("No available server for client request.");
                }
                out.flush();
                s.close();
            } else {
                s.close();
            }
        } catch (Exception e) {
            // Optionally print error
            System.out.println("Error handling connection: " + e.getMessage());
        }
    }

    // Selects a server based on the current load balancing strategy
    private ServerInfo selectServer() {
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
    private void serverStatus(ServerInfo srv) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(srv.socket.getInputStream()));
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equals("FREE")) {
                    srv.busy = false;
                    srv.lastFreeTime = System.currentTimeMillis();
                    System.out.println("Server on port " + srv.port + " is now free.");
                } else if (msg.equals("GOODBYE")) {
                    synchronized (servers) {
                        servers.remove(srv);
                    }
                    srv.socket.close();
                    System.out.println("Server on port " + srv.port + " has disconnected.");
                    break;
                }
            }
        } catch (Exception e) {
            // Remove server if connection is lost
            synchronized (servers) {
                servers.remove(srv);
            }
            System.out.println("Lost connection to server on port " + srv.port + ". Removed from pool.");
        }
    }

    // Simple server metadata class
    private static class ServerInfo {

        int port;
        String strategy;
        boolean busy;
        Socket socket;
        long lastFreeTime;

        ServerInfo(int p, String s, boolean b, Socket sk) {
            port = p;
            strategy = s;
            busy = b;
            socket = sk;
            lastFreeTime = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) throws IOException {
        // Start both static and dynamic load balancers on different ports
        Thread staticLb = new Thread(() -> {
            try {
                LoadBalancer2 lb = new LoadBalancer2("static", 6789);
                lb.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        staticLb.start();

        Thread dynamicLb = new Thread(() -> {
            try {
                LoadBalancer2 lb = new LoadBalancer2("dynamic", 6790);
                lb.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        dynamicLb.start();
    }
}
