package core;

import core.fields.*;

import org.json.JSONObject;
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

public class Server{
    public JSONObject config;
    public int PORT;

    public ServerSocket serverSocket;

    public static List<Short> sessions = new ArrayList<>();

    public void start() throws IOException{
        serverSocket = new ServerSocket(PORT);
        System.out.println("Waiting for connection");
    }

    public Session connect() throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client " + clientSocket.getInetAddress().getHostAddress() + " connected");

        Session clientSession;
        short sessionCookie;

        sessionCookie = generateCookie();
        sessions.add(sessionCookie);
        return new Session(clientSocket, config, sessionCookie);
    }

    private short generateCookie(){
        return (short)(Math.random() * (Short.MAX_VALUE + 1));
    }

    public static void endSession(short cookie, boolean successfully){
        System.out.println("Thread with session ID " + Short.toString(cookie) + " terminated " + (successfully ? "successfully." : " unsuccessfully."));
        sessions.remove(cookie);
    }

    public static void main(String[] args) throws IOException{
        Server server = new Server();
        server.config = Config.parseConfig();
        server.PORT = server.config.getJSONObject("server").getInt("port");
        server.start();

        Thread clientThread;
        Session clientSession;

        while(true){

            clientSession = server.connect();

            clientThread = new Thread(clientSession);
            clientThread.start();

        }

    }
}
