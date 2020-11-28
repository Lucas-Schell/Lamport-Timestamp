import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static int id;
    private volatile static int clock;
    private static int port;
    private static int chance;
    private static ArrayList<Node> nodes;
    private static Random random;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java Main <config.txt> <id>");
            return;
        }

        File f = new File(args[0]);
        Scanner sc;
        try {
            sc = new Scanner(f);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            return;
        }

        random = new Random();
        nodes = new ArrayList<>();
        clock = 0;
        String multiIp = "224.0.0.2";

        while (sc.hasNextLine()) {
            String[] line = sc.nextLine().split(" ");
            /*if (line[0].equals("1")) {
                multiIp = line[1];
            }*/

            if (line[0].equals(args[1])) {
                id = Integer.parseInt(args[1]);
                port = Integer.parseInt(line[2]);
                chance = (int) (Float.parseFloat(line[3]) * 100);
            } else {
                Node node = new Node(Integer.parseInt(line[0]), line[1], Integer.parseInt(line[2]));
                nodes.add(node);
            }
        }
        new Thread(Main::receiveEvent).start();

        try {
            InetAddress group = InetAddress.getByName(multiIp);
            MulticastSocket socket = new MulticastSocket(3000);

            if (id == 1) {
                System.out.println(multiIp);
                socket.joinGroup(group);
                System.out.println('a');
                int connected = 0;
                while (connected != nodes.size()) {
                    byte[] entrada = new byte[1024];
                    DatagramPacket pacote = new DatagramPacket(entrada, entrada.length);
                    socket.receive(pacote);
                    if ((new String(pacote.getData(), 0, pacote.getLength())).equals("Hello")) {
                        connected++;
                    }
                }
                System.out.println(connected);
                String msg = "Start";
                DatagramPacket message = new DatagramPacket(msg.getBytes(), msg.length(), group, 3000);
                socket.send(message);
            } else {
                String msg = "Hello";
                socket.joinGroup(group);
                DatagramPacket message = new DatagramPacket(msg.getBytes(), msg.length(), group, 3000);
                socket.send(message);

                String conf = "";

                while (!conf.equals("Start")) {
                    byte[] entrada = new byte[1024];
                    DatagramPacket pacote = new DatagramPacket(entrada, entrada.length);
                    socket.receive(pacote);
                    conf = new String(pacote.getData(), 0, pacote.getLength());
                }

            }

            socket.leaveGroup(group);
            System.out.println(id + " saiu");
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void start() {
        for (int i = 0; i < 5; i++) {
            int wait = random.nextInt(500) + 500;
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ignored) {
            }

            boolean localEvent = random.nextInt(100) > chance;

            if (localEvent) {
                localEvent();
            } else {
                if (!sendEvent()) {
                    System.out.println("Falha no envio, encerrando...");
                    return;
                }
            }
        }
        System.out.println("Finalizando...");
        System.exit(0);
    }

    public static void localEvent() {
        addClock(-1, "l");
    }

    public static boolean sendEvent() {
        Node randomNode = nodes.get(random.nextInt(nodes.size()));

        int nodeId = randomNode.getId();
        String nodeIp = randomNode.getIp();
        int nodePort = randomNode.getPort();

        try {
            Socket socket = new Socket(nodeIp, nodePort);
            String message = id + " " + (clock + 1);

            PrintStream out = new PrintStream(socket.getOutputStream());
            out.println(message);

            out.close();
            socket.close();
        } catch (Exception e) {
            return false;
        }

        addClock(-1, "s " + nodeId);

        return true;
    }

    public static void receiveEvent() {
        while (true) {
            try {
                ServerSocket servsock = new ServerSocket(port);
                Socket socket = servsock.accept();
                Scanner scanner = new Scanner(socket.getInputStream());

                String receivedId = scanner.next();
                int receivedClock = scanner.nextInt();

                scanner.close();
                socket.close();
                servsock.close();

                addClock(receivedClock, "r " + receivedId + " " + receivedClock);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void addClock(int received, String out) {
        if (received == -1) {
            clock++;
        } else {
            clock = Math.max(clock + 1, received + 1);
        }
        System.out.println(System.nanoTime() + " " + id + " " + clock + id + " " + out);
    }
}
