import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Hydrogen{


    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter N Requests: ");
        long numRequests = scanner.nextLong();

        for (long i = 1; i <= numRequests; i++) {
            System.out.printf(
                    "H%d, request, %s%n",
                    i,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                            .format(new Date())
            );
        }

    }

}
