import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Element {

    private Socket clientSocket;
    private String element;


    public Element(Socket clientSocket, String element) {
        this.clientSocket = clientSocket;
        this.element = element;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getElement() {
        return element;
    }
}
