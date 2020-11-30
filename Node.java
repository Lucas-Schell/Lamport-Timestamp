public class Node {

    private final int port;

    public int getStartPort() {
        return startPort;
    }

    private final int startPort;
    private final String ip;
    private final int id;

    public Node(int id, String ip, int port,int startPort) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.startPort = startPort;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return "ID: " + id + ", IP: " + ip + ", Port: " + port;
    }
}
