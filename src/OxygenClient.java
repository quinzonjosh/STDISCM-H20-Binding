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

public class OxygenClient {

    private static int oxygenCount;
    private static final int nThreads = 8;
    private static final String BINDER_SERVER_ADDRESS = "localhost";
    private static final int BINDER_SERVER_PORT = 4999;

    public static void main(String[] args) {
        String masterAddress;

        // Check if a custom address was provided as an argument
        if (args.length > 0) { // Check if there is at least one argument
            masterAddress = args[0];
        } else {
            masterAddress = BINDER_SERVER_ADDRESS;
        }


        getUserInput();
        generateServerListener(masterAddress);
        generateMolecules();
    }

    private static void generateServerListener(String masterAddress) {
        //listen for server port
        Thread serverListener = new Thread(() -> {
            try (Socket socket = new Socket(masterAddress, 4999)) {
                System.out.println("Connected to Master Server at " + masterAddress + ":4999");

                try(DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                    while (true){
                        String log = dis.readUTF();
                        System.out.println("From Server: " + log);
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        });
        serverListener.start();
    }

    private static void generateMolecules() {
        int batch = oxygenCount / nThreads;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        List<Future<Void>> futures = new ArrayList<>();

        // create threads that independently generate molecules and send out to server
        for(int i = 0; i < nThreads; i++){
            final int start = i * batch;
            final int end = (i == nThreads - 1) ? oxygenCount : (i + 1) * batch;

            try (Socket binderSocket = new Socket(BINDER_SERVER_ADDRESS, BINDER_SERVER_PORT);
                 DataOutputStream dos = new DataOutputStream(binderSocket.getOutputStream())){
                Callable<Void> task = () -> {

                    dos.writeUTF("Oxygen");
                    //send out oxygen molecules one by one
                    for(int j = start; j < end; j++){
                        System.out.println("Generating oxygen O"+j);
                        dos.writeUTF("O"+j);
                    }
                    return null;
                };

                futures.add(executorService.submit(task));

            } catch (IOException e){
                e.printStackTrace();
            }

        }

        for(Future<Void> future : futures){
            try {
                future.get();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        executorService.shutdown();
    }

    private static void getUserInput() {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter number of oxygen Molecules to send: ");
        oxygenCount = input.nextInt();
    }

}
