package org.wstone.distributed;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    public static void main(String[] args) throws IOException {
        generateServers();
    }

    static void generateServers() throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Server server1 = new Server(6001);
        Server server2 = new Server(6002);

        executorService.submit(() -> {
            try {
                server1.listen();
            } catch (IOException e) {
                System.err.println("Error running server1: " + e.getMessage());
            }
        });

        executorService.submit(() -> {
            try {
                server2.listen();
            } catch (IOException e) {
                System.err.println("Error running server2: " + e.getMessage());
            }
        });
    }


    int port;
    ServerSocket ss;

    public Server(int port) throws IOException {
        this.port = port;
        ss = new ServerSocket(port);
    }


    /*
     *
     * Listens for a packet to be sent, the packet contains all the info to do compute heat transfer,
     * which it will do then send back the grid.
     *
     * then the grid will be sent back to the organizer
     *
     */
    void listen() throws IOException {
        try{

            ExecutorService executor = Executors.newSingleThreadExecutor();

            System.out.println("Server is listening on port " + port);

            while(true){
                Socket cs = ss.accept();
                System.out.println("Client connected: " + cs.getInetAddress());
                executor.submit(() -> processOrganizerConnection(cs));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            ss.close();
        }
    }

    private static void processOrganizerConnection(Socket cs){
        try{
            ObjectInputStream inputStream = new ObjectInputStream(cs.getInputStream());

            ObjectOutputStream outputStream = new ObjectOutputStream(cs.getOutputStream());

            Packet packet = (Packet) inputStream.readObject();

            System.out.println("Received Packet from client:");

            packet.computeHeatTransfer();

            // send the same packet back to the organizer.
            // the organizer will then take that packet, then send the updated packet containing the grid, which we will be waiting to receive
            outputStream.writeObject(packet);
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
