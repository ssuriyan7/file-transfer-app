File transfer application using socket programming.

The application is implemented in Java and consists of three classes. 

FtpServer: Implements the server side functions of the application.
FtpClient: Implements the client side functions of the application.
FtpRequest: A model used to transfer request message from client to server.

To run the application:

1. Compilation: application contains only two source files
    > javac FtpServer.java
    > javac FtpCLient.java

Running above two commands gives 4 class files.
    FtpServer.class
    FtpClient.class
    FtpRequest.class
    RequestType.class

2. Start the server:
    > java FtpServer

    This starts the server on localhost and port 8000 on default. You could specify a custom port number as follows:

    > java FtpServer 8080

3. Start the client program:
    > java FtpCLient <server_port_number>
    Eg: java FtpCLient 8000

4. The client can take three different commands:

    i. get <download_filename>
    ii. upload <upload_filename>
    iii. exit

    Running one of the above commands will send appropriate request to the server.

