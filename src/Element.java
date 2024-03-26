import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Element {

    private OutputStream clientOutputStream;
    private String element;


    public Element(OutputStream clientOutputStream, String element) {
        this.clientOutputStream = clientOutputStream;
        this.element = element;
    }

    public OutputStream getClientOutputStream() {
        return clientOutputStream;
    }

    public String getElement() {
        return element;
    }
}
