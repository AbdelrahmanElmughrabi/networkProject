package newPackageoOop;

import java.io.*;
import java.net.*;
import java.util.*;

public class LoadBalancer2 {

    private List<ServerInfo> servers = new ArrayList<>();
    private int port = 6789;
    private int rrIndex = 0;

    public LoadBalancer2(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Load Balancer listening on port " + port);
        while (true) {
            Socket s = ss.accept();
            System.out.println("Accepted connection from: " + s.getRemoteSocketAddress());
            new Thread(() -> handle(s)).start();
        }
    }

    // Handles both server registration and client requests
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
                String[] parts = msg.split(" ");
                String requestType = parts.length > 1 ? parts[1].toLowerCase() : "";
                String chosenStrategy;
                switch (requestType) {
                    case "file":
                    case "directory":
                        chosenStrategy = "static";
                        break;
                    case "stream":
                    case "compute":
                    case "computation":
                        chosenStrategy = "dynamic";
                        break;
                    default:
                        chosenStrategy = "static";
                }
                ServerInfo selected = selectServer(chosenStrategy);
                if (selected != null) {
                    selected.busy = true;
                    out.writeBytes(selected.port + "\n");
                    System.out.println("Assigned client to server on port " + selected.port + " (" + chosenStrategy + ")");
                } else {
                    out.writeBytes("NO_SERVER\n");
                    System.out.println("No available server for client request (" + chosenStrategy + ").");
                }
                out.flush();
                s.close();
            } else {
                s.close();
            }
        } catch (Exception e) {
            System.out.println("Error handling connection: " + e.getMessage());
        }
    }

    // Selects a server based on the request strategy (static: round-robin, dynamic: least-connections)
    private ServerInfo selectServer(String strategy) {
        synchronized (servers) {
            if ("static".equalsIgnoreCase(strategy)) {
                // Round-robin for static servers
                List<ServerInfo> candidates = new ArrayList<>();
                for (ServerInfo s : servers) {
                    if (!s.busy && "static".equalsIgnoreCase(s.strategy)) {
                        candidates.add(s);
                    }
                }
                if (candidates.isEmpty()) {
                    return null;
                }
                int pick = rrIndex % candidates.size();
                ServerInfo chosen = candidates.get(pick);
                rrIndex = (rrIndex + 1) % candidates.size();
                chosen.busy = true;
                return chosen;
            } else if ("dynamic".equalsIgnoreCase(strategy)) {
                // Least-connections for dynamic servers
                List<ServerInfo> candidates = new ArrayList<>();
                for (ServerInfo s : servers) {
                    if (!s.busy && "dynamic".equalsIgnoreCase(s.strategy)) {
                        candidates.add(s);
                    }
                }
                if (candidates.isEmpty()) {
                    return null;
                }
                ServerInfo best = candidates.get(0);
                for (ServerInfo s : candidates) {
                    if (s.currentConnections < best.currentConnections) {
                        best = s;
                    } else if (s.currentConnections == best.currentConnections) {
                        if (s.lastFreeTime < best.lastFreeTime) {
                            best = s;
                        }
                    }
                }
                best.busy = true;
                best.currentConnections++;
                return best;
            }
            return null;
        }
    }

    // Listens for status updates from a registered server (FREE or GOODBYE)
    private void serverStatus(ServerInfo srv) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(srv.socket.getInputStream()));
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equals("FREE")) {
                    srv.busy = false;
                    srv.lastFreeTime = System.currentTimeMillis();
                    if ("dynamic".equalsIgnoreCase(srv.strategy)) {
                        srv.currentConnections = Math.max(0, srv.currentConnections - 1);
                    }
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
            synchronized (servers) {
                servers.remove(srv);
            }
            System.out.println("Lost connection to server on port " + srv.port + ". Removed from pool.");
        }
    }

    // Holds metadata for each registered server
    private static class ServerInfo {

        int port;
        String strategy;
        boolean busy;
        Socket socket;
        long lastFreeTime;
        int currentConnections = 0;

        ServerInfo(int p, String s, boolean b, Socket sk) {
            port = p;
            strategy = s;
            busy = b;
            socket = sk;
            lastFreeTime = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) throws IOException {
        LoadBalancer2 lb = new LoadBalancer2(6789);
        lb.start();
    }
}
