import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class OxygenClient {
    public static int oxygenCount;
    private DataInputStream dis;
    private DataOutputStream dos;
    private int SERVER_PORT;
    private String SERVER_ADDRESS;

    public OxygenClient(String SERVER_ADDRESS, int SERVER_PORT) {
        this.SERVER_ADDRESS = SERVER_ADDRESS;
        this.SERVER_PORT = SERVER_PORT;
    }

    private void start() {
        try {
            Socket serverSocket = new Socket(this.SERVER_ADDRESS, this.SERVER_PORT);
            System.out.println("Connected to server.");

            this.dos = new DataOutputStream(serverSocket.getOutputStream());

            getUserInput();
            sendOxygenMolecules();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendOxygenMolecules() {
        try {
            dos.writeUTF("Oxygen");

            for (int i = 0; i < oxygenCount; i++) {
                dos.writeUTF("O" + i);
                dos.flush();
            }
            dos.writeUTF("DONE");
            dos.flush();
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getUserInput() {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter number of Oxygen Molecules to send: ");
        oxygenCount = input.nextInt();
    }

    public static void main(String[] args) {
        int SERVER_PORT = 4999;
        String SERVER_ADDRESS = "localhost";

        OxygenClient oxygenClient = new OxygenClient(SERVER_ADDRESS, SERVER_PORT);
        oxygenClient.start();
    }
}
