import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client {
    private Scanner reader = new Scanner(System.in);

    private String getServerAddress() {
        System.out.println("Give the IP of server");
        return reader.next();
    }

    public void run() throws IOException {
        BufferedReader in;
        PrintWriter out;

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return ;
            }
//            System.out.println("\n" + line + "!\n");
            if (line != null) {
                if (line.startsWith("SEND"))
                    System.out.println(line.substring(5));
                else if (line.equals("WAIT_INPUT"))
                    out.println(reader.next());
                else if (line.equals("NEW_LINE"))
                    System.out.println();
                else if (line.equals("CLEAR"))
                    clearScreen();
                else if (line.startsWith("SLEEP")) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(Integer.parseInt(line.substring(6)));
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                }
                else
                    System.out.println("Server message is not a valid message");
            }
            else {
                System.out.println("An error occurs");
                return ;
            }
        }
    }

    private void clearScreen() {
        System.out.println("Try to clear screen.");
        String os = System.getProperty("os.name");

        try {
            if (os.contains("Windows")) {
                // Don't work :/
                Runtime.getRuntime().exec("cls");
            } else {
                System.out.println("\033[2J");
            }
        }
        catch (IOException e) {}
    }
}
