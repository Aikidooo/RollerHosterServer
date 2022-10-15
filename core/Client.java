package core;

import java.net.*;
import java.io.*;

//THIS IS JUST TEMPORARY TEST CLIENT FOR TESTING THE SERVER!!!

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws IOException{
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }
    public String receiveMessage() throws IOException{
        return in.readLine();
    }

    public void stopConnection() throws IOException{
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args){
        Client client = new Client();
        try {
            client.startConnection("localhost", 6969);
            System.out.println("Sending authentication token");
            client.sendMessage("NiceToEatYou");

        } catch(IOException e){
            e.printStackTrace();
        }
        try {
            System.out.println("Sending ");
            client.sendMessage("I WANT ALL");
            System.out.println("Awaiting server index files");
            String r = client.receiveMessage();
            System.out.println("Server has " + r);
            System.out.println("Sending needed files");
            System.out.println(client.receiveMessage());

        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
