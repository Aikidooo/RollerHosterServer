package core;

import core.fields.*;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

//GitHub: https://github.com/Aikidooo/RollerHoster
//Protocol reference:  https://github.com/Aikidooo/RollerHoster/blob/master/protocoll.txt

public class Server {
    private ServerSocket server;
    private Socket clientSocket;
    private OutputStream out;
    private BufferedInputStream in;


    private static final int PORT = 6969;
    private short cookie = 0b0000000000000000;

    private boolean fileStreamOpen = false;

    private HashMap<String, byte[]> files = new HashMap<String, byte[]>();

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
        Packet token = receive();
        System.out.println(token.getStringData());
        if(!isAuth(token.getStringData())){
            System.out.println("Client is not authenticated, quitting...");
            return false;
        }
        if(token.state != States.Authentication){
            System.out.println("Client is in wrong state, quitting...");
            return false;
        }
        System.out.println("Client authenticated successfully");
        cookie = generateCookie();

        //(0:2)
        send(Short.toString(cookie), States.Authentication);
        return true;
    }

    public boolean fileTreeExchange() throws IOException{
        //(1:1)
        Packet clientRequest = receive();
        if(!isValidCookie(clientRequest.getShortCookie())){
            System.out.println("Client provided invalid cookie, quitting...");
            return false;
        }
        if(clientRequest.state != States.ExchangeFiletree || clientRequest.data[0] != 0){
            System.out.println("Client is in wrong state, quitting...");
            return false;
        }

        String contents = Files.readString(Paths.get("resources/FileIndex.txt"));
        System.out.println("Sending filetree to Client");
        //(1:2)
        send(contents, States.ExchangeFiletree);
        return true;
    }

    public boolean fileDownload() throws IOException{

        //(2:1)
        Packet fileRequestHeader = receive(); //Header packet

        while (fileRequestHeader.data[0] != DebugCodes.FileDownloadEnd && fileRequestHeader.state != States.Debug) {

            if (!isValidCookie(fileRequestHeader.getShortCookie())) {
                System.out.println("Client provided invalid cookie, quitting...");
                return false;
            }
            if (fileRequestHeader.state != States.FileDownload) {
                System.out.println("Client is in wrong state, quitting...");
                return false;
            }
            if (!(fileRequestHeader.data[0] > 8)) {
                System.out.println("Client provided invalid chunk format, quitting...");
                return false;
            }

            byte[] data = fileRequestHeader.data;

            byte chunkCount = data[0];

            byte[] bFilename = Arrays.copyOfRange(data, 1, data.length);
            String filename = new String(bFilename, StandardCharsets.UTF_8);


            files.put(filename, ByteUtils.readFileToBytes(Paths.get("Shared/" + filename)));

            int chunkSize = files.get(filename).length / chunkCount;

            //(2:2)
            Packet chunkRequest = receive();

            while (chunkRequest.data[0] != DebugCodes.FileEnd && chunkRequest.state != States.Debug) {
                byte chunkId = chunkRequest.data[0];
                byte[] chunk = Arrays.copyOfRange(files.get(filename), chunkSize * (chunkId - 1), chunkSize * chunkId);

                Packet chunkPacket = new Packet((short) (ByteUtils.PAYLOAD_LENGTH + chunk.length), States.FileDownload, (short) 0, chunk);
                //(2:3)
                send(chunkPacket);
                chunkRequest = receive();
            }

            fileRequestHeader = receive();
        }
        return true;
    }

    public boolean fileUpload() throws IOException{
        Packet header = receive();

        if(!isValidCookie(header.getShortCookie())){
            System.out.println("Client provided invalid cookie, quitting...");
            return false;
        }
        if(header.state != States.FileUpload){
            System.out.println("Client is in wrong state, quitting...");
            return false;
        }

        byte chunkCount = header.data[0];
        byte[] bFilename = Arrays.copyOfRange(header.data, 1, header.data.length);
        String filename = new String(bFilename, StandardCharsets.UTF_8);

        try {
            File file = new File(filename);
            if(file.createNewFile()){
                System.out.println("Created " + filename);
            } else {
                System.out.println(filename + " exists already");
            }

            FileWriter fileWriter = new FileWriter(filename);

        } catch (IOException e){
            System.out.println("Could not create " + filename);
            e.printStackTrace();
        }


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

            return authToken.equals(token);

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
        start(PORT);

        while (true) {
            try {
                connect();

                //Stage authentication
                if(!authenticate()) {
                    stop();
                    continue;
                }
                //Stage exchange filetree
                if(!fileTreeExchange()){
                    stop();
                    continue;
                }
                //Stage file download
                fileStreamOpen = true;
                while(fileDownload() && fileStreamOpen);
                if(fileStreamOpen){
                    stop();
                    continue;
                }
                //Stage file upload


            } catch (IOException e) {
                e.printStackTrace();
                stop();
            }
        }
    }

    public static void main(String[] args) throws IOException{
        Server server = new Server();
        server.runProtocol();

        System.out.println("Finished Interaction");
    }
}
