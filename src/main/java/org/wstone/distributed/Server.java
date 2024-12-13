package org.wstone.distributed;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    public static void main(String[] args) throws IOException {
        Server s = new Server();
        s.listen();
    }
    int PORT = 6966;
    ServerSocket ss;


    /*
     * Listens for incoming connections and when something is first received from the organizer,
     * it sets grid equal to that, and sends the updated map back to the organizer.
     *
     * The organizer then sets the shared map equal to the map it received, then sends it back
     */
    void listen() throws IOException {
        try{
            ss = new ServerSocket(PORT);

            ExecutorService executor = Executors.newSingleThreadExecutor();

            System.out.println("Server is listening on port " + PORT);

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

            Region[][] receivedRegions = (Region[][]) inputStream.readObject();

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


    class Region implements Serializable {
        CopyOnWriteArrayList<Simulation.Alloy.Region> neighbors;
        double thermalCoefficient;
        private volatile double temperature;
        ReentrantLock lock;

        // we assume any region is made of 3 alloys with 3 different thermal coefficients with a 20% variance in the amount each alloy.
        // in this context we can interpret that the variance scales linearly to the thermal coefficient
        public Region(double c1, double c2, double c3){
            neighbors = new CopyOnWriteArrayList<>();
            lock = new ReentrantLock();
            // 20% variation
            thermalCoefficient = (c1*generateUniformRandom(0.8, 1.2) + c2*generateUniformRandom(0.8, 1.2) + (c3*generateUniformRandom(0.8, 1.2))) / 3;
        }

        static double generateUniformRandom(double min, double max) {
            return min + (max - min) * ThreadLocalRandom.current().nextDouble();
        }

        void setTemperature(double temperature) {
            lock.lock();
            try {
                this.temperature = temperature;
            }finally {
                lock.unlock();
            }
        }
    }
}
