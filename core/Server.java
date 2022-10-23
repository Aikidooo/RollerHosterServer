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
    public JSONObject config;
    public int PORT;

    public ServerSocket serverSocket;

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


        sessions.add(clientSession);
        return clientSession;
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
