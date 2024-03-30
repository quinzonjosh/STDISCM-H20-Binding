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

public class HydrogenClient {
    public static int hydrogenCount;
    private DataInputStream dis;
    private DataOutputStream dos;
    private int SERVER_PORT;
    private String SERVER_ADDRESS;
    public static final int NTHREADS = 8;

    private final BlockingQueue<Interval> intervals = new LinkedBlockingQueue<>();

    private List<Thread> threads = new ArrayList<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");
    public HydrogenClient(String SERVER_ADDRESS, int SERVER_PORT){
        this.SERVER_ADDRESS = SERVER_ADDRESS;
        this.SERVER_PORT = SERVER_PORT;
    }

    private void start() {
        try{
            Socket serverSocket = new Socket(this.SERVER_ADDRESS, this.SERVER_PORT);
            System.out.println("Connected to server.");

            this.dos = new DataOutputStream(serverSocket.getOutputStream());
            this.dis = new DataInputStream(serverSocket.getInputStream());

            Thread serverListener = new Thread(new ServerMessageReceiver(dis));
            serverListener.start();


            getUserInput();
            sendHydrogenMolecules();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendHydrogenMolecules() {
        int batch = hydrogenCount / NTHREADS;


        for (int i = 0; i < NTHREADS; i++) {
            Thread thread = new Thread(new DataSender(intervals));
            threads.add(thread);
            thread.start();
        }

        try {
            dos.writeUTF("Hydrogen");
            for (int i = 0; i < NTHREADS; i++) {
                final int start = i * batch;
                final int end = (i == NTHREADS - 1) ? hydrogenCount : (i + 1) * batch;

                synchronized (intervals) {
                    intervals.offer(new Interval(start, end, false));
                    intervals.notifyAll();
                }
            }

            synchronized (intervals) {
                intervals.offer(new Interval(-1, -1, true));
                intervals.notifyAll();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // Handle InterruptedException
                    e.printStackTrace();
                }
            }

            dos.writeUTF("DONE");
        } catch (IOException e){
            e.printStackTrace();
        }



//        ExecutorService executorService = Executors.newFixedThreadPool(NTHREADS);

//        try {
//            dos.writeUTF("Hydrogen");
//
//            for(int i = 0; i < NTHREADS; i++) {
//                final int start = i * batch;
//                final int end = (i == NTHREADS - 1) ? hydrogenCount : (i + 1) * batch;
//
//                System.out.printf("(%d, %d)%n", start, end);
//
//                executorService.submit(() -> {
//                    for(int j = start; j < end; j++){
//                        try {
//                            String element = "H"+j;
//                            dos.writeUTF(element);
////                            dos.flush();
//                            String log = element + ", requested, " + LocalDateTime.now().format(FORMATTER);
//                            System.out.println(log);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                });
//            }
//
//            executorService.shutdown();
//
//            // Wait indefinitely for all tasks to complete
//            try {
//                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                // (Re-)Cancel if current thread also interrupted
//                executorService.shutdownNow();
//                // Preserve interrupt status
//                Thread.currentThread().interrupt();
//            }
//
//
//            dos.writeUTF("DONE");
//            dos.flush();
////            dos.close();
//        } catch (IOException e){
//            e.printStackTrace();
//        }
    }


    private class DataSender implements Runnable{

        private final BlockingQueue<Interval> intervals;

        public DataSender(BlockingQueue<Interval> intervals) {
            this.intervals = intervals;
        }

        @Override
        public void run() {
            synchronized (intervals) {
                while (true) {
                    while (intervals.isEmpty()) {
                        try {
                            intervals.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    Interval interval = intervals.poll();
                    assert interval != null;

                    if (interval.isLast()) {
                        synchronized (intervals) {
                            intervals.offer(interval);
                            intervals.notifyAll();
                        }
                        break;
                    }

                    for(int j = interval.getStart(); j < interval.getEnd(); j++){
                        try {
                            String element = "H"+j;
                            dos.writeUTF(element);
//                            dos.flush();
                            String log = element + ", requested, " + LocalDateTime.now().format(FORMATTER);
                            System.out.println(log);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
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
