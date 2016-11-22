import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = 4444;
        VFServer server = new VFServer(port);
        server.run();

    }
}
