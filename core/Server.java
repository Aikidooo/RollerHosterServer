package core;

import core.fields.States;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;


public class Server {
    private ServerSocket server;
    private Socket clientSocket;
    private OutputStream out;
    private BufferedInputStream in;

    private static final int PORT = 6969;
    private short cookie = 0b0000000000000000;

    private void send(String content, byte state) throws IOException{
        byte[] bContent = content.getBytes(StandardCharsets.UTF_8);

        byte[] payload = new byte[ByteUtils.PAYLOAD_LENGTH];

        short packetLength = (short)(ByteUtils.PAYLOAD_LENGTH + bContent.length);
        short cookie = 0b0000000000000000; //not necessary in send

        String binaryPacketLength = Integer.toBinaryString(0xFFFF & packetLength);
        String binaryCookie = "0000000000000000";

        payload[0] = Byte.parseByte(binaryPacketLength.substring(0, 8));
        payload[1] = Byte.parseByte(binaryPacketLength.substring(8, 16));
        payload[2] = state;
        payload[3] = Byte.parseByte(binaryCookie.substring(0, 8));
        payload[4] = Byte.parseByte(binaryCookie.substring(8, 16));

        //Concatenating payload and contents together
        byte[] both = Arrays.copyOf(payload, payload.length + bContent.length);
        System.arraycopy(bContent, 0, both, payload.length, bContent.length);

        out.write(both);
    }

    private String receive() throws IOException{
        byte[] rawData = new byte[10000];
        int readNum = in.read(rawData);
        byte[] data = Arrays.copyOfRange(rawData, 0, readNum);


        short packetLength = (short)(((short)data[0] << 8) | (short) data[1]); //Probably no work
        byte state = data[2];
        short cookie = (short)(((short)data[3] << 8) | (short) data[4]); //Probably also no work
        byte[] content = Arrays.copyOfRange(data, 5, data.length);

        if(!isValidCookie(cookie) && state != States.Authentication) stop();

        return new String(content, StandardCharsets.UTF_8);
    }

    public boolean start(int port) throws IOException{
        System.out.println("Waiting for connection");
        server = new ServerSocket(port);
        clientSocket = server.accept();

        System.out.println("Client " + clientSocket.getInetAddress() + " connected");

        in = new BufferedInputStream(clientSocket.getInputStream());
        out = clientSocket.getOutputStream();

        //Stage Authentication
        String authentication = receive();
        System.out.println(authentication);
        if(!isAuth(authentication)){
            System.out.println("Client is not authenticated, quitting...");
            stop();
            return false;
        }
        System.out.println("Client authenticated successfully");
        cookie = generateCookie();
        send(Short.toString(cookie), States.Authentication);
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
        //Stage send File Indexes
        String contents = Files.readString(Paths.get("resources/FileIndex.txt"));
        System.out.println("Sending file indexes to Client");
        send(new String(contents), States.SyncCheck);

        //Stage receive needed Filenames
        System.out.println("Waiting for needed filelist");
        String neededFiles = receive();

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

        /*try{
            server.runProtocol();
        } catch (IOException e){
            e.printStackTrace();
        }*/
        System.out.println("Finished Interaction");

    }
}
