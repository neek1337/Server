import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class VFServer {
    public static final Object lock = new Object();
    public static boolean alreadyUsed = false;
    public static String activeUserName;
    public static AtomicInteger roundCount = new AtomicInteger(0);
    public static AtomicInteger roundResult = new AtomicInteger(0);
    public static int index = 0;
    private int serverPort;
    private ServerSocket serverSocket = null;
    static final Set<Client> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static String alphabet = " абвгдежзийклмнопрстуфхцчшщьыэюя";
    public static ArrayList<AtomicInteger> results = new ArrayList<>();
    public static int messageLength = 0;

    VFServer(int port) {
        this.serverPort = port;
    }

    void run() throws IOException {
        try {
            openServerSocket();
        } catch (RuntimeException e) {
            System.out.println("Данный порт уже используется");
            return;
        }
        System.out.println("Сервер запущен");
        while (true) {
            Socket clientSocket;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException | NullPointerException e) {
                System.out.println("Ошибка в соединении с клиентом");
                return;
            }
            new Thread(new Client(clientSocket)).start();
        }
    }


    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}