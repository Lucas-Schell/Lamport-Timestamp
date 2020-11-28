public class Node {

    private final int port;
    private final String ip;
    private final int id;

    public Node(int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
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
