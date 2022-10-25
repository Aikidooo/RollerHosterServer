package core;

import core.fields.Config;

import core.fields.States;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

//GitHub: https://github.com/Aikidooo/RollerHoster
//Protocol reference:  https://github.com/Aikidooo/RollerHoster/blob/master/protocoll.txt

public class Server{
    private JSONObject config;
    private int PORT;

    private ServerSocket serverSocket;

    public static List<Session> sessions = new ArrayList<>();

    public void start() throws IOException{
        serverSocket = new ServerSocket(PORT);
        System.out.println("Waiting for connection");

        config = Config.parseConfig();
        PORT = config.getJSONObject("server").getInt("port");
    }

    public Session connect() throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client " + clientSocket.getInetAddress().getHostAddress() + " connected");

        short sessionCookie = generateCookie();
        Session clientSession = new Session(clientSocket, config, sessionCookie);


<<<<<<< HEAD
        sessions.add(clientSession);
        return clientSession;
=======
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

    public void stop() throws IOException{
        in.close();
        out.close();
        clientSocket.close();
        cookie = 0b0000000000000000;
    }

    private boolean isAuth(String token) {
        return token.equals(TOKEN);
>>>>>>> d6be8e6e0456e4cf457e29e775f70013b2b3451d
    }

    private short generateCookie(){
        return (short)(Math.random() * (Short.MAX_VALUE + 1));
    }

    public static void endSession(short cookie, boolean successfully){
        System.out.println("Thread with session ID " + cookie + " terminated " + (successfully ? "successfully." : " unsuccessfully."));
        sessions.remove(cookie);
    }

    public static void printSessions(){
        System.out.println("Current sessions:\n");
        for(Session s : sessions){
            System.out.println(s.getClientAddr());
            System.out.println("\tSessionId: " + s.getCookie());
            System.out.println("\tState: " + States.get(s.getState()) + "(" + s.getState() + ")");
        }
    }

    public static void main(String[] args) throws IOException{
        Server server = new Server();
        server.start();

        Thread clientThread;
        Session clientSession;

        while(true){

            clientSession = server.connect();

            clientThread = new Thread(clientSession);
            clientThread.start();

            printSessions();
        }

    }
}
