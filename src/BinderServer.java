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

                setupInputAndOutputStreams(clientSocket);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    private void setupInputAndOutputStreams(Socket clientSocket) {
        try {
            this.dis = new DataInputStream(clientSocket.getInputStream());
//            this.dos = new DataOutputStream(clientSocket.getOutputStream());

            getDataFromClients();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getDataFromClients() {
        try {
            int hydrogenCount = dis.readInt();
            for (int i=0; i<hydrogenCount; i++){
                hydrogenQueue.offer(dis.readUTF());
            }

            sanityCheck();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sanityCheck() {
        System.out.println("hydrogen queue: " + hydrogenQueue);

    }

    public static void main(String[] args) {
        int SERVER_PORT = 4999;
        BinderServer binderServer = new BinderServer(SERVER_PORT);
        binderServer.start();
    }

}
