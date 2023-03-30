import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

// class that manages a client's connection and processes requests in a separate thread.
// server creates and runs a thread of this class type when a client initiates connection
class ClientRequestProcessor implements Runnable {

    private Socket                  socket;               // server side socket
    private ObjectInputStream       inputStream;          // stream to receive data from client
    private ObjectOutputStream      outputStream;         // stream to send data to client


    public ClientRequestProcessor(Socket socket) throws Exception {

        this.socket             = socket;
        this.outputStream       = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        this.inputStream        = new ObjectInputStream(socket.getInputStream());

        System.out.println("Connected to: " + socket.getInetAddress().getHostAddress());
        
    }


    @Override
    public void run() {

        try {
            while (true) {
                System.out.println("Waiting for request from client.");

                //fetch the request command from the client
                FtpRequest request;
                try {
                    request = (FtpRequest) inputStream.readObject();
                } catch (EOFException eofException) {
                    System.out.println("Client closed connection.");
                    break;
                }
                if (request != null) {
                    
                    //based on the request type execute appropriate actions: upload or receive file
                    if (request.requestType == RequestType.UPLOAD) {

                        System.out.println("Received file upload request from client: " + request.filename);
    
                        FileOutputStream outStream = new FileOutputStream("new_" + request.filename);
                        long bytesLeft = request.length;
                        int bytesRead = 0;
                        byte bytes[] = new byte[1024];
                        while (bytesLeft > 0 && (bytesRead = inputStream.read(bytes, 0, (int)Math.min(1024, bytesLeft))) != -1) {
                            outStream.write(bytes, 0, bytesRead);
                            bytesLeft -= bytesRead;
                        }
                        System.out.println("Upload complete.");
                        outStream.close();
    
                    } else if (request.requestType == RequestType.GET) {

                        System.out.println("Received file download request from client: " + request.filename);
    
                        FileInputStream fileStream = new FileInputStream(request.filename);
                        byte[] bytes = new byte[1024];
                        int sizeRead = 0;
                        while ((sizeRead = fileStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, sizeRead);
                            outputStream.flush();
                        }
                        System.out.println("Download complete.");
                        fileStream.close();
    
                    } else {
                        System.out.println("Invalid request from client.");
                    }
                }
            }

            socket.close();
            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            System.out.println("Exception occured in client thread." + e);
            e.printStackTrace();
        }
    }
}

// server class that accepts connections and spawns a thread for each client
public class FtpServer {
    
    private int                     port     = 8000;      // port on which the server accepts connections
    private ServerSocket            server;               // server object to accept connections

    public FtpServer() {
    }
    
    // initializes the server on localhost and the given port
    public boolean init(String[] args) {

        try {
            if (args.length == 1) {
                this.port = Integer.parseInt(args[0]);
            }
            server = new ServerSocket(this.port, 10, InetAddress.getLoopbackAddress());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    // server functions for uploading and receiving files from client
    public void start() throws Exception {

        while (true) {

            // accept a connection from client
            Socket socket          = server.accept();

            // spawn request processor thread for this connection
            Thread processorThread = new Thread(new ClientRequestProcessor(socket));

            // start the thread
            processorThread.start();

        }

    }

    public static void main(String[] args) {
        
        FtpServer server = new FtpServer();
        
        try {
            //initial server setup
            boolean status = server.init(args);

            if (status) {
                System.out.println("Server is ready to accept connections on port: " + server.port);
            } else {
                System.out.println("Failed to initialize server on port: " + server.port);
            }

            // server is ready to process client requests
            server.start();

        } catch (Exception e) {
            System.out.println("Unexpected exception on server. ");
            e.printStackTrace();
        } finally {
            try {
                server.server.close();
            } catch(IOException ioEx) {
                System.out.println("Errored while closing connections.");
                ioEx.printStackTrace();
            }
        }
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