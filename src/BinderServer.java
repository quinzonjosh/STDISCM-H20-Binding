import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BinderServer {

    private static final ExecutorService clientExecutor = Executors.newCachedThreadPool();
//    private static final ExecutorService slaveExecutor = Executors.newCachedThreadPool();
//    private static final List<SlaveInfo> slaves = new CopyOnWriteArrayList<>();
    private static final int BINDER_SERVER_PORT = 4999;

    private static final int CLIENT_PORT = 4999;
//    private static final int SLAVE_REGISTRATION_PORT = 5001;
    private static BlockingQueue<Element> hydrogenQueue = new LinkedBlockingQueue<>();
    private static BlockingQueue<Element> oxygenQueue = new LinkedBlockingQueue<>();

    private static final int MAX_HYDROGEN_TO_BOND = 2;

    private static final int MAX_OXYGEN_TO_BOND = 1;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm:ss");

    public static void main(String[] args) {
        // Separate thread for listening to slave server registrations
        Thread bindingThread = new Thread(BinderServer::checkBond);
        bindingThread.start();

        // declare a server socket host
        try (ServerSocket serverSocket = new ServerSocket(CLIENT_PORT)) {
            System.out.println("Master Server Listening for clients on port " + CLIENT_PORT);
            // continuously listen for client requests
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            clientExecutor.shutdown();
//            slaveExecutor.shutdown();
        }

    }

    private static void checkBond() {
        while (true){
            if (hydrogenQueue.size() >= MAX_HYDROGEN_TO_BOND && oxygenQueue.size() >= MAX_OXYGEN_TO_BOND){

                List<Element> elements = new ArrayList<>();
                elements.add(hydrogenQueue.poll());
                elements.add(hydrogenQueue.poll());
                elements.add(oxygenQueue.poll());

                bond(elements);
            }
        }
    }

    private static void bond(List<Element> elements) {
        elements.forEach(element -> {
            try {
                DataOutputStream dos = new DataOutputStream(element.getClientOutputStream());
                String log = element.getElement() + ", bonded, " + LocalDateTime.now().format(FORMATTER);
                System.out.println("Server: " + log);
                dos.writeUTF(log);
            }catch (IOException e){
                e.printStackTrace();
            }
        });
    }


    private static void handleClient(Socket clientSocket) {


        try{
            //setup writer & reader
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

            // acquire data from client
            String type = dis.readUTF(); // to check whether oxygen or hydrogen client

            if(type.equals("Oxygen")){
                while (true){
                    String element = dis.readUTF();
                    oxygenQueue.add(new Element(clientSocket.getOutputStream(), element));
                }
            }
            else {
                while (true){
                    String element = dis.readUTF();
                    hydrogenQueue.add(new Element(clientSocket.getOutputStream(), element));
                }
            }


            //adding data
            /* while true
            * 1. If type == 'Oxygen' add to oxygenqueue  --> needed of executor
            * 2. If type == 'Hydrogen' add to hydrogenqueue --> needed of executor
            *
            * while true --> in another thread
            * bonding
            *
            *
            *
            *
            * */
            // bonding
            //

        } catch (IOException e){
            System.err.println("Error handling client: " + clientSocket);
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }




}


