package core;

import javax.security.sasl.AuthenticationException;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;

public class Server {
    private ServerSocket server;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private static final int PORT = 6969;

    private void send(String content){
        out.println(content);
    }

    private String receive() throws IOException{
        return in.readLine();
    }

    public boolean start(int port) throws IOException{
        System.out.println("Waiting for connection");
        server = new ServerSocket(port);
        clientSocket = server.accept();

        System.out.println("Client " + clientSocket.getInetAddress() + " connected");

        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream());

        //Stage Authentication
        String authentication = receive();
        if(!isAuth(authentication)){
            stop();
            System.out.println("Client is not authenticated, quitting...");
            send("Your not Authenticated");
            return false;
        }
        return true;
    }

    public void stop() throws IOException{
        in.close();
        out.close();
        clientSocket.close();
        server.close();
    }

    private boolean isAuth(String authToken) {
        try {
            FileReader f = new FileReader("resources/token.txt", StandardCharsets.UTF_8);

            char[] token = new char[100];
            f.read(token);

            if (authToken.equals(new String(token))){
                return true;
            } else{
                return false;
            }

        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void runProtocol() throws IOException{
        //Stage send File Indexes
        FileReader f = new FileReader("FileIndex.txt", StandardCharsets.UTF_8);
        char[] contents = new char[1000];
        f.read(contents);

        send(new String(contents));

        //Stage receive needed Filenames
        String neededFiles = receive();
        System.out.println(neededFiles);
        //TODO: Send files back
    }

    public static void main(String[] args){
        boolean connected = false;
        while(!connected) {
            try {
                Server server = new Server();
                connected = server.start(PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Yee");

    }
}
