# Network Project: Load Balancer with Static and Dynamic Strategies

## 1. Load Balancing Strategies Implemented

- **Static Strategy (Round-Robin):**
  - For requests such as file transfer and directory listing, the load balancer uses a round-robin algorithm to distribute clients evenly among all available static servers.
- **Dynamic Strategy (Least-Connections with LRU Tie-Breaker):**
  - For computation-heavy or streaming requests, the load balancer assigns clients to dynamic servers using a least-connections algorithm. If multiple servers have the same number of active connections, the server that has been idle the longest (Least Recently Used) is chosen.

## 2. High Level Approach

- **Protocols Used:**  
  - All communication between clients, load balancer, and servers is done over **TCP sockets** for reliable, ordered delivery.

- **Application Layer Mechanisms:**
  - **Server Registration:** Servers register themselves with the load balancer, specifying their port and strategy (static/dynamic).
  - **Client Request Routing:** Clients send a request type to the load balancer, which selects an appropriate server based on the request and current load.
  - **Server Availability Tracking:** Servers notify the load balancer when they are free to accept new requests.
  - **Unified Port:** Both static and dynamic strategies are handled through a single load balancer port, simplifying client and server configuration.

- **Design Features:**
  - **Strategy-Aware Routing:** The load balancer automatically determines the appropriate strategy based on the client’s request type.
  - **Concurrency:** The system supports multiple concurrent clients and servers.
  - **Extensibility:** The design allows for easy addition of new strategies or server types.
  - **Robustness:** The load balancer handles server disconnects and notifies clients if no servers are available.

## 3. Challenges Faced

- **Static and Dynamic Strategies:** Refactoring the load balancer to handle both strategies through a single port and shared server pool required careful redesign.
- **Concurrency Management:** Ensuring thread safety when multiple clients and servers interact with the load balancer simultaneously.
- **Server Availability Tracking:** Accurately tracking when servers become free, especially for long-running or resource-intensive requests.
- **Protocol Consistency:** Designing a simple, robust protocol for communication between all components.

## 4. Testing

- **Automated Testing:** The client includes a test mode that can automatically launch 100 concurrent clients to stress-test the system.
- **Manual Testing:** The system was also tested interactively by sending different types of requests and verifying correct routing and server assignment.
- **Edge Cases:** Tested scenarios where all servers are busy, servers disconnect unexpectedly, and clients request unsupported operations.

## 5. How to Run the Project

1. **Start the Load Balancer:**  
   Run the `LoadBalancer2` class. This will open the unified port and wait for server registrations and client requests.
2. **Start the Servers:**  
   Run the `Server2` class. You can start multiple servers; each will register itself with the load balancer and wait for client connections.
3. **Start the Clients:**  
   Run the `Client2` class. You can use interactive mode for manual requests or test mode to launch multiple clients automatically.

**Order of Execution:**  
1. Load Balancer  
2. Server(s)  
3. Client(s)

---

**Note:**  
- Ensure all components are running on the same machine or adjust the hostnames/ports as needed.
- The system is designed for easy extension and further experimentation with load balancing algorithms.