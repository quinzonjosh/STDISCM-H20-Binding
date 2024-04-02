import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OxygenClient {
    public static int oxygenCount;
    private DataInputStream dis;
    private DataOutputStream dos;
    public static final int NTHREADS = 8;
    private int SERVER_PORT;
    private String SERVER_ADDRESS;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss.SSS");

    private final BlockingQueue<Interval> intervals = new LinkedBlockingQueue<>();
    private List<Thread> threads = new ArrayList<>();

    private Set<String> sentRequests = ConcurrentHashMap.newKeySet();
    private Set<String> confirmedBonds = ConcurrentHashMap.newKeySet();

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

    private class ServerMessageReceiver implements Runnable{

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

                    Pattern pattern = Pattern.compile("O(\\d+)");
                    Matcher matcher = pattern.matcher(log);

                    if (matcher.find()) {
                        String number = matcher.group(1);
                        String elementId = matcher.group(0);
                        if(sentRequests.contains(elementId) && !confirmedBonds.contains(elementId)) {
                            confirmedBonds.add(elementId); // Validate and track confirmation
//                            System.out.println("Confirmed bonding for: " + elementId);
                        } else {
                            // Log error due to duplicate or premature confirmation
                            System.out.println("Error: Duplicate or premature bond confirmation received for " + elementId);
                        }
                        if(Integer.parseInt(number) == oxygenCount - 1){
                            System.out.println("--- SANITY CHECK: OxygenClient ---");
                            if(sentRequests.equals(confirmedBonds)) {
                                System.out.println("All sent requests were confirmed correctly.");
                            } else {
                                System.out.println("Mismatch between sent requests and confirmed bonds.");
                            }
                            System.out.println("No. of sent requests: " + sentRequests.size());
                            System.out.println(("No. of confirmed bonds: " + confirmedBonds.size()));
                            System.exit(0);
                        }
                    }
                } catch (IOException e){
                    e.printStackTrace();
                    break;
                }
            }

        }
    }

    private void sendOxygenMolecules() {
        int batch = oxygenCount / NTHREADS;


        for (int i = 0; i < NTHREADS; i++) {
            Thread thread = new Thread(new DataSender(intervals));
            threads.add(thread);
            thread.start();
        }

        try {
            dos.writeUTF("Oxygen");
            for (int i = 0; i < NTHREADS; i++) {
                final int start = i * batch;
                final int end = (i == NTHREADS - 1) ? oxygenCount : (i + 1) * batch;

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
                            String element = "O"+j;
                            sentRequests.add(element);
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
