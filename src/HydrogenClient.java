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
    private static int hydrogenCount;
    private static final int nThreads = 8;
    private static final String BINDER_SERVER_ADDRESS = "localhost";
    private static final int BINDER_SERVER_PORT = 4999;

    public static void main(String[] args) {
        getUserInput();
        generateMolecules();
    }

    private static void generateMolecules() {
        int batch = hydrogenCount / nThreads;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        List<Future<Void>> futures = new ArrayList<>();

        // create threads that independently generate molecules and send out to server
        for(int i = 0; i < nThreads; i++){
            final int start = i * batch;
            final int end = (i == nThreads - 1) ? hydrogenCount : (i + 1) * batch;

            try (Socket binderSocket = new Socket(BINDER_SERVER_ADDRESS, BINDER_SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(binderSocket.getOutputStream())){
                Callable<Void> task = () -> {
                    //send out hydrogen molecules one by one
                    for(int j = start; j < end; j++){
                        System.out.println("Generating hydrogen H"+j);
                        dos.writeChars("H"+j);
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
        System.out.print("Enter number of Hydrogen Molecules to send: ");
        hydrogenCount = input.nextInt();
    }
}
