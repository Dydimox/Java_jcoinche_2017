import java.io.IOException;

public class Application {
    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.run();
        } catch (IOException e) {
            System.out.println("Exception handled");
            System.out.println(e.getMessage());
        }
    }
}
