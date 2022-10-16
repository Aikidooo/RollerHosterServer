package core;

import core.fields.*;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

//GitHub: https://github.com/Aikidooo/RollerHoster
//Protocol reference:  https://github.com/Aikidooo/RollerHoster/blob/master/protocoll.txt

public class Server {
    private ServerSocket server;
    private Socket clientSocket;
    private OutputStream out;
    private BufferedInputStream in;


    private static final int PORT = 6969;
    private short cookie = 0b0000000000000000;

    private void send(String content, byte state) throws IOException{
        byte[] bContent = content.getBytes(StandardCharsets.UTF_8);
        System.out.println(new String(bContent, StandardCharsets.UTF_8));

        Packet packet = new Packet((short)(ByteUtils.PAYLOAD_LENGTH + content.length()), state, cookie, bContent);

        out.write(packet.get());
    }

    private void send(Packet packet) throws IOException{
        out.write(packet.get());
    }

    private Packet receive() throws IOException{
        byte[] rawData = new byte[10000];
        int readNum = in.read(rawData);
        byte[] data = Arrays.copyOfRange(rawData, 0, readNum);

        Packet packet = new Packet(data);

        if(!isValidCookie(packet.getShortCookie()) && packet.state != States.Authentication) {
            stop();
        }

        return packet;
    }

    public void start(int port) throws IOException{
        server = new ServerSocket(port);
        System.out.println("Waiting for connection");
    }

    public void connect() throws IOException {
        clientSocket = server.accept();
        System.out.println("Client " + clientSocket.getInetAddress() + " connected");

        in = new BufferedInputStream(clientSocket.getInputStream());
        out = clientSocket.getOutputStream();
    }

    //Protocol (0:X)
    public boolean authenticate() throws IOException{
        //(0:1)
        String token = receive().getStringData();
        System.out.println(token);
        if(!isAuth(token)){
            System.out.println("Client is not authenticated, quitting...");
            return false;
        }
        System.out.println("Client authenticated successfully");
        cookie = generateCookie();

        //(0:2)
        send(Short.toString(cookie), States.Authentication);
        return true;
    }

    public boolean fileTreeExchange() throws IOException{

        String contents = Files.readString(Paths.get("resources/FileIndex.txt"));
        System.out.println("Sending filetree to Client");
        send(new String(contents), States.SyncCheck);
        return true;
    }

    public void stop() throws IOException{
        in.close();
        out.close();
        clientSocket.close();
        server.close();
        cookie = 0b0000000000000000;
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

    private short generateCookie(){
        return (short)(Math.random() * (Short.MAX_VALUE + 1));
    }

    private boolean isValidCookie(short cookie){
        return cookie == this.cookie;
    }

    public void runProtocol() throws IOException{
        boolean ready = false;

        start(PORT);


        while (true) {

            try {
                connect();

                //Stage Authentication
                if(!authenticate()) {
                    stop();
                    continue;
                }

                //Stage exchange filetree


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws IOException{
        Server server = new Server();
        server.runProtocol();
        /*try{
            server.runProtocol();
        } catch (IOException e){
            e.printStackTrace();
        }*/
        System.out.println("Finished Interaction");

    }
}
