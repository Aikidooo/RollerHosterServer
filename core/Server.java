package core;

import core.fields.Config;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


//GitHub: https://github.com/Aikidooo/RollerHoster
//Protocol reference:  https://github.com/Aikidooo/RollerHoster/blob/master/protocoll.txt

public class Server{
    ///////////////DECLARATION///////////////

    private JSONObject config;
    private int PORT;

    private ServerSocket serverSocket;

    ///////////////INITIALIZATION///////////////

    public static List<Session> sessions = new ArrayList<>();
    private final Logger logger = new Logger("Server");

    public void start() throws IOException{
        config = Config.parseConfig();
        PORT = config.getJSONObject("server").getInt("port");

        logger.log("DEBUG", "Starting on port " + PORT);
        serverSocket = new ServerSocket(PORT);
        logger.log("INFO", "Waiting for connection");

    }

    ///////////////NETWORK///////////////

    public Session accept() throws IOException {
        Socket clientSocket = serverSocket.accept();
        logger.log("INFO", "Client " + clientSocket.getInetAddress().getHostAddress() + " connected");

        short sessionCookie = generateCookie();
        Session clientSession = new Session(clientSocket, config, sessionCookie);


        sessions.add(clientSession);
        return clientSession;
    }

    private short generateCookie(){
        return (short)(Math.random() * (Short.MAX_VALUE + 1));
    }

    ///////////////THREADING///////////////

    public static void endSession(Session session, boolean successfully){
        Logger.logGlobal((successfully ? "INFO" : " WARNING"), "Thread with session ID " + session.getCookie() + " terminated " + (successfully ? "successfully." : " unsuccessfully."));
        sessions.remove(session);
    }

    public static void main(String[] args) throws IOException{
        Server server = new Server();
        server.start();

        Thread clientThread;
        Session clientSession;

        while(true){
            clientSession = server.accept();
            Logger.printSessions();
            clientThread = new Thread(clientSession);
            clientThread.start();

        }

    }
}
