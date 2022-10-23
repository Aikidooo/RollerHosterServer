package core;

import core.fields.DebugCodes;
import core.fields.States;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class Session implements Runnable{

    ///////////////DECLARATION///////////////

    private final JSONObject config;
    //private int PORT;
    private String TOKEN;
    private String SHARED_DIR;

    private final Socket clientSocket;
    private final OutputStream out;
    private final BufferedInputStream in;

    ///////////////INITIALIZATION///////////////

    private short cookie;

    private final HashMap<String, byte[]> files = new HashMap<>();

    Session(Socket clientSocket, JSONObject config, short cookie) throws IOException{
        this.clientSocket = clientSocket;
        this.config = config;
        this.cookie = cookie;

        init();

        in = new BufferedInputStream(clientSocket.getInputStream());
        out = clientSocket.getOutputStream();
    }

    @Override
    public void run(){
        try {
            boolean successful = runProtocol();
            Server.endSession(cookie, successful);
        } catch (IOException e) {
            e.printStackTrace();
            Server.endSession(cookie, false);
        }
    }

    private void init() throws IOException{

        //JSONObject server = config.getJSONObject("server");
        JSONObject authentication = config.getJSONObject("authentication");
        JSONObject environment = config.getJSONObject("environment");

        //PORT = server.getInt("port");
        TOKEN = authentication.getString("token");
        SHARED_DIR = environment.getString("shared_dir");
    }

    public void close() throws IOException{
        in.close();
        out.close();
        clientSocket.close();
    }

    ///////////////NETWORK-COMMUNICATION///////////////

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
            close();
        }

        return packet;
    }

    ///////////////PROTOCOL-EXECUTION///////////////

    public boolean runProtocol() throws IOException{
        try {
            //Stage authentication
            if(!authenticate()) {
                close();
                return false;
            }
            //Stage exchange filetree
            if(!fileTreeExchange()){
                close();
                return false;
            }
            //Stage file download
            if(!fileDownload()){
                close();
                return false;
            }
            //Stage file upload
            if(!fileUpload()){
                close();
                return false;
            }
            return false;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            close();
        }
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

        //(0:2)
        send(Short.toString(cookie), States.Authentication);
        return true;
    }
    //Protocol (1:X)
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

        FileTree fileTree = new FileTree(SHARED_DIR);
        fileTree.map();
        fileTree.safe("resources/FileIndex.txt");
        String contents = fileTree.get();
        System.out.println("Sending filetree to Client");
        //(1:2)
        send(contents, States.ExchangeFiletree);
        return true;
    }
    //Protocol (2:X)
    public boolean fileDownload() throws IOException{

        //(2:1)
        Packet fileRequestHeader = receive(); //Header packet

        //loop through all file requests until the debug packet is sent:
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


            files.put(filename, ByteUtils.readFileToBytes(Paths.get(SHARED_DIR + filename)));

            int chunkSize = files.get(filename).length / chunkCount;

            //(2:2)
            Packet chunkRequest = receive();
            //loop through all chunks of current file until the debug packet is sent
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
        files.clear();
        return true;
    }
    //Protocol (3:X)
    public boolean fileUpload() throws IOException{
        //(3:1)
        Packet fileUploadHeader = receive();


        while (fileUploadHeader.data[0] != DebugCodes.FileDownloadEnd && fileUploadHeader.state != States.Debug) {
            if (!isValidCookie(fileUploadHeader.getShortCookie())) {
                System.out.println("Client provided invalid cookie, quitting...");
                return false;
            }
            if (fileUploadHeader.state != States.FileUpload) {
                System.out.println("Client is in wrong state, quitting...");
                return false;
            }

            byte[] data = fileUploadHeader.data;

            byte chunkCount = data[0];

            byte[] bFilename = Arrays.copyOfRange(data, 1, data.length);
            String filename = new String(bFilename, StandardCharsets.UTF_8);

            try {
                File file = new File(filename);
                if (file.createNewFile()) {
                    System.out.println("Created " + filename);
                } else {
                    System.out.println(filename + " exists already");
                }

            } catch (IOException e) {
                System.out.println("Could not create " + filename);
                e.printStackTrace();
                continue;
            }

            FileOutputStream fileWriter = new FileOutputStream(filename, true);

            Packet chunkUpload = receive();

            while (chunkUpload.data[0] != DebugCodes.FileEnd && chunkUpload.state != States.Debug) {
                byte[] uploadData = chunkUpload.data;

                byte chunkId = uploadData[0];
                byte[] chunkData = Arrays.copyOfRange(uploadData, 1, uploadData.length);

                fileWriter.write(chunkData);

                chunkUpload = receive();
            }

        }
        return true;
    }

    ///////////////VALIDATION///////////////

    private boolean isValidCookie(short cookie){
        return cookie == this.cookie;
    }

    private boolean isAuth(String token) {
        return token.equals(TOKEN);
    }
}
