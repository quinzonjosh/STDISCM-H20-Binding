import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BinderServer {
    private ServerSocket serverSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private BlockingQueue<String> hydrogenQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<String> oxygenQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        int SERVER_PORT = 4999;
        BinderServer binderServer = new BinderServer(SERVER_PORT);
        binderServer.start();
    }

    public BinderServer(int port){
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void start() {
        System.out.println("Server is running & listening for connections...");
        while(true){
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client " + clientSocket + " connected");

                Thread clientHandler = new Thread(new ClientHandler(clientSocket));
                clientHandler.start();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void sanityCheck() {
        System.out.println("hydrogen queue: " + hydrogenQueue);
        System.out.println("Oxygen queue: " + oxygenQueue);
    }

    private class ClientHandler implements Runnable{
        private Socket clientSocket;
        private DataInputStream dis;
        private DataOutputStream dos;

        public ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            try {
                this.dis = new DataInputStream(clientSocket.getInputStream());
                this.dos = new DataOutputStream(clientSocket.getOutputStream());

                String clientType = dis.readUTF();

                switch (clientType) {
                    case "Hydrogen":
                        handleHydrogenClient();
                        break;
                    case "Oxygen":
                        handleOxygenClient();
                        break;
                    default:
                        System.out.println("Unknown client type");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleOxygenClient() throws IOException {
            while (true) {
                String molecule = dis.readUTF();
                if (molecule.equals("DONE")) {
                    break;
                } else {
                    oxygenQueue.offer(molecule);
                }
            }
            sanityCheck();
        }

        private void handleHydrogenClient() throws IOException {
            while (true) {
                String molecule = dis.readUTF();
                if (molecule.equals("DONE")) {
                    break;
                } else {
                    hydrogenQueue.offer(molecule);
                }
            }
            sanityCheck();
        }
    }


}
