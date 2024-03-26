import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BinderServer {
    private static final int BINDER_SERVER_PORT = 4999;
    BlockingQueue<String> hydrogenQueue = new LinkedBlockingQueue<>();
    BlockingQueue<String> oxygenQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {


    }

}
