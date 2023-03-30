import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;


public class FtpClient {

    private int                     port;           // port to which the client connects to
    private Socket                  socket;         // client side socket
    private ObjectOutputStream      outputStream;   // stream to send data to server
    private ObjectInputStream       inputStream;    // stream to receive data from server

    private FtpRequest              request;

    public FtpClient(int port) {
        this.port = port;
    }

    // establishes connection to server and initializes the streams for data transfer
    public void init() throws Exception {
        socket           = new Socket(InetAddress.getLoopbackAddress(), port);
        inputStream      = new ObjectInputStream(socket.getInputStream());
        outputStream     = new ObjectOutputStream(socket.getOutputStream());
    }

    // executes the command from user
    public void runCommand(String command) throws Exception {

        String action       = command.split(" ")[0];

        FtpRequest request  = new FtpRequest();
        if ("get".equals(action)) {
            request.requestType = RequestType.GET;
        } else if ("upload".equals(action)) {
            request.requestType = RequestType.UPLOAD;
        }

        request.filename = command.split(" ")[1];
        File file = new File(request.filename);
        request.length = file.length();

        this.request = request;

        outputStream.writeObject(this.request);
        outputStream.flush();

        if ("get".equals(action)) {
            downloadFile();
        } else if ("upload".equals(action)) {
            uploadFile();
        }

    }

    // uploads a given file in chunks to the outputstream of the socket
    public void uploadFile() throws Exception{
        FileInputStream fileStream = new FileInputStream(request.filename);
        byte[] bytes = new byte[1024];
        int sizeRead = 0;
    
        while ((sizeRead = fileStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, sizeRead);
            outputStream.flush();
        }
        System.out.println("Upload complete.");
        fileStream.close();
    }

    // downloads a given file in chunks from the inputstream of the socket
    public void downloadFile() throws Exception{
        FileOutputStream outStream = new FileOutputStream("new_" + request.filename);
        long bytesLeft = request.length;
        int bytesRead = 0;
        byte bytes[] = new byte[1024];

        while (bytesLeft > 0 && (bytesRead = inputStream.read(bytes, 0, (int)Math.min(1024, bytesLeft))) != -1) {
            outStream.write(bytes, 0, bytesRead);
            bytesLeft -= bytesRead;
        }
        System.out.println("Download complete.");
        outStream.close();
    }

    public static void main(String[] args) {

        try {

            Scanner scanner = new Scanner(System.in);
            int port;
            if (args.length != 1 || (Integer.parseInt(args[0]) < 1024 || Integer.parseInt(args[0]) > 65535)) {
                // loop until user enters a valid port
                while (true) {
                    System.out.println("Invalid input.");
                    System.out.print("Enter a valid port number (1024 - 65535): ");
                    port = scanner.nextInt();
                    if (port >= 1024 && port <= 65535) {
                        break;
                    }
                }

            } else {
                port = Integer.parseInt(args[0]);
            }
    
            // initialize the client and connect to the server
            FtpClient client = new FtpClient(port);
            client.init();
            
            while (true) {
                boolean validInput;
                String command;
                
                do {

                    System.out.print("Enter a command: ");
                    command     = scanner.nextLine();
                    validInput  = validateInput(command);
    
                } while (!validInput);
    
                if ("exit".equals(command.split(" ")[0])) {
                    break;
                }

                client.runCommand(command);

            }
            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    // validates the command provided by user
    static boolean validateInput(String command) {
        
        boolean result = true;
        String[] parts = command.split(" ");
        if (parts.length != 2) {
            if (parts.length == 1 && "exit".equals(parts[0])) {
                result = true;
                return result;
            } else {
                System.out.println("Invalid command.");
                result = false;
            }
        } else if (!("get".equals(parts[0])  || "upload".equals(parts[0]))) {
            System.out.println("Invalid action.");
            result = false;
        } 

        File file = new File(parts[1]);
        if (!file.exists()) {
            System.out.println("Input file does not exist.");
            result = false;
        }
        if (!result) {
            System.out.println("Valid command usage: \n get <valid_file_name> \n upload <valid_file_name>");
        }

        return result;

    }

}

enum RequestType {
    GET,
    UPLOAD,
    EXIT
}

class FtpRequest implements Serializable {

    RequestType     requestType;        // get, upload
    String          filename;           // filename to be used with the request
    long            length;             // length of the file

    public FtpRequest() {
    }

    public FtpRequest(RequestType requestType, String filename, long length) {
        this.requestType = requestType;
        this.filename = filename;
        this.length = length;
    }

}