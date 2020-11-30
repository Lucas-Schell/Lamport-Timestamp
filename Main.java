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

        // le arquivo de configuração
        while (sc.hasNextLine()) {
            String[] line = sc.nextLine().split(" ");

            //Caso a linha do arquivo de configuração seja o proprio nodo, guarda as informações nas variáveis globais. Se não guarda na lista de nodos.
            if (line[0].equals(args[1])) {
                id = Integer.parseInt(args[1]);
                port = Integer.parseInt(line[2]);
                chance = (int) (Float.parseFloat(line[3]) * 100);
            } else {
                Node node = new Node(Integer.parseInt(line[0]), line[1], Integer.parseInt(line[2]));
                nodes.add(node);
            }
        }

        // inicia o método para receber mensagem de outros nodos em uma thread paralela
        new Thread(Main::receiveEvent).start();

        try {
            // o nodo com id 1 irá esperar até todos os outros nodos se conectarem
            // e mandarem um Hello para mandar um Start e sair do loop
            if (id == 1) {
                Socket socket;
                PrintStream out;
                for (Node node : nodes) {
                    socket = new Socket(node.getIp(), node.getPort());
                    out = new PrintStream(socket.getOutputStream());
                    out.println("s");

                    out.close();
                    socket.close();
                }
            } else {
                ServerSocket servsock = new ServerSocket(port);
                Socket socket = servsock.accept();
                Scanner scanner = new Scanner(socket.getInputStream());

                scanner.next();

                scanner.close();
                socket.close();
                servsock.close();

            }

            System.out.println(id + " começou...");

            // começa a gerar os eventos
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void start() {
        for (int i = 0; i < 5; i++) {
            // espera entre 0,5 e 1 segundo para gerar um evento
            int waitTime = random.nextInt(500) + 500;
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ignored) {
            }

            // escolhe randomicamente se o evento será local ou não de acordo com a chance do nodo
            boolean localEvent = random.nextInt(100) > chance;
            if (localEvent) {
                localEvent();
            } else {
                // caso não tenha uma resposta do outro nodo encerra a execução
                if (!sendEvent()) {
                    System.out.println("Falha no envio, encerrando...");
                    break;
                }
            }
        }
        System.out.println("Finalizando...");
        System.exit(0);
    }

    // gera um evento local
    public static void localEvent() {
        addClock(-1, "l");
    }

    //escolhe um nodo aleatório e envia a id e o relogio atual para ele
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

    // espera conexões de outros nodos
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

    // realiza as operações no relógio local e faz o print das informações
    public synchronized static void addClock(int received, String out) {
        if (received == -1) {
            clock++;
        } else {
            clock = Math.max(clock, received) + 1;
        }
        System.out.println(System.nanoTime() + " " + id + " " + clock + id + " " + out);
    }
}
