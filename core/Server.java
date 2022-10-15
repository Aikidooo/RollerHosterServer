package core;


import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

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
            System.out.println("Client is not authenticated, quitting...");
            send("Your not Authenticated");
            stop();
            return false;
        }
        System.out.println("Client authenticated successfully");
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
            String token = Files.readString(Paths.get("resources/token.txt"), StandardCharsets.UTF_8);

            if (authToken.equals(token)){
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
        String contents = Files.readString(Paths.get("resources/FileIndex.txt"));
        System.out.println("Sending file indexes to Client");
        send(new String(contents));

        //Stage receive needed Filenames
        System.out.println("Waiting for needed filelist");
        String neededFiles = receive();
        System.out.println(neededFiles);
        //TODO: Send files back
        send("ALL");
        System.out.println("Sent desired files");
    }

    public static void main(String[] args){
        boolean connected = false;
        Server server = new Server();

        while(!connected) {
            try {
                connected = server.start(PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try{
            server.runProtocol();
        } catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("Finished Interaction");

    }
}
