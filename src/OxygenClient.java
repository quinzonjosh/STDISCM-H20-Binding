import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class OxygenClient {
    public static int oxygenCount;
    private DataInputStream dis;
    private DataOutputStream dos;
    public static final int NTHREADS = 8;
    private int SERVER_PORT;
    private String SERVER_ADDRESS;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");

    public OxygenClient(String SERVER_ADDRESS, int SERVER_PORT) {
        this.SERVER_ADDRESS = SERVER_ADDRESS;
        this.SERVER_PORT = SERVER_PORT;
    }

    private void start() {
        try {
            Socket serverSocket = new Socket(this.SERVER_ADDRESS, this.SERVER_PORT);
            System.out.println("Connected to server.");

            this.dos = new DataOutputStream(serverSocket.getOutputStream());
            this.dis = new DataInputStream(serverSocket.getInputStream());

            Thread serverListener = new Thread(new ServerMessageReceiver(dis));
            serverListener.start();

            getUserInput();
            sendOxygenMolecules();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ServerMessageReceiver implements Runnable{

        private DataInputStream dis;

        public ServerMessageReceiver(DataInputStream dis) {
            this.dis = dis;
        }

        @Override
        public void run() {
            while (true){
                try {
                    String log = dis.readUTF();
                    System.out.println("From Server: " + log);
                } catch (IOException e){
                    e.printStackTrace();
                    break;
                }
            }

        }
    }

    private void sendOxygenMolecules() {
        int batch = oxygenCount / NTHREADS;
        ExecutorService executorService = Executors.newFixedThreadPool(NTHREADS);

        try {
            dos.writeUTF("Oxygen");

            for(int i = 0; i < NTHREADS; i++) {
                final int start = i * batch;
                final int end = (i == NTHREADS - 1) ? oxygenCount : (i + 1) * batch;

                System.out.printf("(%d, %d)%n", start, end);

                executorService.submit(() -> {
                    for(int j = start; j < end; j++){
                        try {
                            String element = "O"+j;
                            dos.writeUTF(element);
//                            dos.flush();
                            String log = element + ", requested, " + LocalDateTime.now().format(FORMATTER);
                            System.out.println(log);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            executorService.shutdown();

            // Wait indefinitely for all tasks to complete
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // (Re-)Cancel if current thread also interrupted
                executorService.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }


            dos.writeUTF("DONE");
            dos.flush();
//            dos.close();
        } catch (IOException e){
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
