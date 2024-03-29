import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HydrogenClient {
    public static int hydrogenCount;
    private DataInputStream dis;
    private DataOutputStream dos;
    private int SERVER_PORT;
    private String SERVER_ADDRESS;
    public static final int NTHREADS = 8;
    public HydrogenClient(String SERVER_ADDRESS, int SERVER_PORT){
        this.SERVER_ADDRESS = SERVER_ADDRESS;
        this.SERVER_PORT = SERVER_PORT;
    }

    private void start() {
        try{
            Socket serverSocket = new Socket(this.SERVER_ADDRESS, this.SERVER_PORT);
            System.out.println("Connected to server.");

            this.dos = new DataOutputStream(serverSocket.getOutputStream());

            getUserInput();
            sendHydrogenMolecules();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendHydrogenMolecules() {
        try {
            dos.writeInt(hydrogenCount);

            for (int i=0; i<hydrogenCount; i++){
                dos.writeUTF("H"+i);
                dos.flush();
            }
            dos.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

//    private void sendHydrogenMolecules() {
//        int batch = hydrogenCount / NTHREADS;
//        ExecutorService executorService = Executors.newFixedThreadPool(NTHREADS);
//
//        for(int j=0; j<NTHREADS; j++){
//            final int start = j * batch;
//            final int end = (j == NTHREADS - 1) ? hydrogenCount : (j + 1) * batch;
//
//            executorService.execute(()->{
//                try{
//                    for(int i=start; i<=end; i++){
//                        dos.writeUTF("H"+i);
//                        dos.flush();
//                        System.out.println("sent H" + i);
//                    }
//
//                    dos.writeUTF("DONE");
//                    dos.flush();
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
//            });
//        }
//
//        executorService.shutdown();
//        while (!executorService.isTerminated()){
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    private static void getUserInput() {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter number of Hydrogen Molecules to send: ");
        hydrogenCount = input.nextInt();
    }

    public static void main(String[] args) {
        int SERVER_PORT = 4999;
        String SERVER_ADDRESS = "localhost";

        HydrogenClient hydrogenClient = new HydrogenClient(SERVER_ADDRESS, SERVER_PORT);
        hydrogenClient.start();
    }
}
