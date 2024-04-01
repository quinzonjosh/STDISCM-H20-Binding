import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BinderServer {
    private ServerSocket serverSocket;
    private BlockingQueue<Element> hydrogenQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Element> oxygenQueue = new LinkedBlockingQueue<>();

    private static final int MAX_HYDROGEN_TO_BOND = 2;
    private static final int MAX_OXYGEN_TO_BOND = 1;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss.SSS");

    private Set<String> requestedElements = Collections.synchronizedSet(new HashSet<>());
    private Set<String> bondedElements = Collections.synchronizedSet(new HashSet<>());
    private AtomicInteger errorCount = new AtomicInteger();

    private AtomicInteger totalHydrogenReceived = new AtomicInteger(0);
    private AtomicInteger totalOxygenReceived = new AtomicInteger(0);

    public static void main(String[] args) {
        int SERVER_PORT = 4999;
        BinderServer binderServer = new BinderServer(SERVER_PORT);

        // Adding a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            binderServer.reportSanityCheckStatus();
        }));

        binderServer.start();
    }

    public void reportSanityCheckStatus() {
        int expectedBondCount = correctBondCount();
        System.out.println("--- SANITY CHECK STATUS ---");
        System.out.println("Errors identified: " + errorCount.get());
        System.out.println(requestedElements.isEmpty() && bondedElements.size() == expectedBondCount ? "All bonds are correct and accounted for." : "There are discrepancies in bonds.");

        System.out.println("Expected bond count: " + expectedBondCount);
        System.out.println("All bond requests accounted for: " + requestedElements.isEmpty());
        System.out.println("No. of bonded elements: " + bondedElements.size());
    }

    public int correctBondCount() {
        int hydrogenAtoms = totalHydrogenReceived.get();
        int oxygenAtoms = totalOxygenReceived.get();

        // Calculate how many full H2O molecules can be formed
        int totalPossibleMolecules = Math.min(hydrogenAtoms / 2, oxygenAtoms);

        return totalPossibleMolecules * 3; // Total atoms involved in bonding
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


        Thread bindingThread = new Thread(new BindingHandler(hydrogenQueue, oxygenQueue));
        bindingThread.start();

        while(true){
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client " + clientSocket + " connected");
                Thread clientHandler = new Thread(new ClientHandler(clientSocket));
                clientHandler.start();
            } catch (IOException e){
                e.printStackTrace();
                break;
            }
        }



    }

    private void sanityCheck() {
        List<String> list = new ArrayList<>();
        hydrogenQueue.forEach(element -> list.add(element.getElement()));
        System.out.println("hydrogen queue: " + Arrays.toString(list.toArray()));

        list.clear();
        oxygenQueue.forEach(element -> list.add(element.getElement()));
        System.out.println("Oxygen queue: " + Arrays.toString(list.toArray()));
    }

    private class BindingHandler implements Runnable {

        private BlockingQueue<Element> hydrogenQueue;
        private BlockingQueue<Element> oxygenQueue;

        public BindingHandler(BlockingQueue<Element> hydrogenQueue, BlockingQueue<Element> oxygenQueue) {
            this.hydrogenQueue = hydrogenQueue;
            this.oxygenQueue = oxygenQueue;
        }

        @Override
        public void run() {
            while (true){
                if (hydrogenQueue.size() >= MAX_HYDROGEN_TO_BOND && oxygenQueue.size() >= MAX_OXYGEN_TO_BOND){

                    List<Element> elements = new ArrayList<>();
                    elements.add(hydrogenQueue.poll());
                    elements.add(hydrogenQueue.poll());
                    elements.add(oxygenQueue.poll());

                    // Check if all elements have sent requests before bonding
                    if (elements.stream().allMatch(e -> requestedElements.contains(e.getElement()))) {
                        // Proceed with bonding
                        bond(elements);

                        // After bonding, update sets
                        elements.forEach(e -> {
                            requestedElements.remove(e.getElement());
                            bondedElements.add(e.getElement());
                        });
                    } else {
                        // Log or handle error: Attempted to bond without all elements having sent requests
                        errorCount.incrementAndGet();
                    }
                }
            }
        }

        private void bond(List<Element> elements) {
            elements.forEach(element -> {
                try {
                    DataOutputStream dos = new DataOutputStream(element.getClientSocket().getOutputStream());
                    String log = element.getElement() + ", bonded, " + LocalDateTime.now().format(FORMATTER);
                    System.out.println("Server: " + log);
                    dos.writeUTF(log);



                }catch (IOException e){
                    e.printStackTrace();
                }
            });
        }

    }
    private class ClientHandler implements Runnable{
        private Socket clientSocket;
        private DataInputStream dis;

        public ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            try {
                this.dis = new DataInputStream(clientSocket.getInputStream());

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
            }
//            finally {
//                try {
//                    clientSocket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }

        private void handleOxygenClient() throws IOException {
            while (true) {
                String molecule = dis.readUTF();
                if (molecule.equals("DONE")) {
                    break;
                } else {
                    totalOxygenReceived.incrementAndGet();
                    requestedElements.add(molecule);
                    oxygenQueue.offer(new Element(clientSocket, molecule));
                }
            }
//            sanityCheck();
        }

        private void handleHydrogenClient() throws IOException {
            while (true) {
                String molecule = dis.readUTF();
                if (molecule.equals("DONE")) {
                    break;
                } else {
                    totalHydrogenReceived.incrementAndGet();
                    requestedElements.add(molecule);
                    hydrogenQueue.offer(new Element(clientSocket, molecule));
                }
            }
//            sanityCheck();
        }
    }


}
