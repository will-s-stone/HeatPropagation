package org.wstone.distributed;

import java.io.*;
import java.net.Socket;

public class Organizer {
    public static void main(String[] args) {
        Organizer o = new Organizer();
        Server.Region[][] r = new Server.Region[2][2];
        o.start(r);
    }


    void start(Server.Region[][] grid){
        String host = "localhost";
        int port = 6966;

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {

            // Send the Integer object
            objectOutputStream.writeObject(grid);
            objectOutputStream.flush();

            System.out.println("Sent to " + host + ":" + port);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void processServerConnection(Socket cs){
        try{

            ObjectInputStream inputStream = new ObjectInputStream(cs.getInputStream());

            ObjectOutputStream outputStream = new ObjectOutputStream(cs.getOutputStream());

            Server.Region[][] receivedRegions = (Server.Region[][]) inputStream.readObject();

            System.out.println("Received Region[][] from client:");

            // send the same Region[][] back to the client
            outputStream.writeObject(receivedRegions);
            outputStream.flush();

            outputStream.close();
            inputStream.close();
            cs.close();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client connection error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
