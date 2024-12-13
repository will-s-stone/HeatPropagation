package org.wstone.distributed;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Organizer {
    private final int iterations;
    private Packet curPacket = null;
    Visualization vis;
    private static final int NUM_SERVERS =2;
    private final ExecutorService executorService;
    private int width;
    private int height;
    private Region[][] grid;
    ConcurrentHashMap<String, Double> tempMap = new ConcurrentHashMap<>();

    public Organizer(int height, int width, double s, double t, double c1, double c2, double c3, int iterations){
        this.width = width;
        this.height = height;
        this.grid = new Region[height][width];
        this.iterations = iterations;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new Region(c1, c2, c3);
            }
        }
        vis = new Visualization(this.tempMap, height, width, s, t);

        setUpNeighbors();
        this.executorService = Executors.newFixedThreadPool(NUM_THREADS);
        initializeHeatSources(s, t);
    }

    private Packet sendAndReceive(String serverAddress, int serverPort, Packet packet) throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            // Send the packet to the server
            outputStream.writeObject(packet);
            outputStream.flush();

            // Wait for and read the processed packet back
            try {
                Packet processedPacket = (Packet) inputStream.readObject();
                return processedPacket;
            } catch (ClassNotFoundException e) {
                // Convert to IOException to maintain method signature
                throw new IOException("Error deserializing packet", e);
            }
        }
    }

    void simulateHeatTransfer(JFrame frame) throws IOException {
        frame.setSize((int) (vis.getWidth())+3, (int) (vis.getHeight()+30));
        frame.add(vis);
        frame.setVisible(true);
        /*
         * each iteration computes the heat transfer fo
         */
        for (int iteration = 0; iteration < iterations; iteration++) {
            CountDownLatch latch = new CountDownLatch(NUM_SERVERS);
            int rowsPerThread = height/NUM_SERVERS;
            for (int threadIndex = 0; threadIndex < NUM_SERVERS; threadIndex++) {
                final int startRow = threadIndex * rowsPerThread;
                final int endRow = (threadIndex == NUM_SERVERS - 1) ? height : startRow + rowsPerThread;
                executorService.submit(() -> {
                    try{
                        //computeHeatTransfer(startRow, endRow);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
            if(iteration % 5 == 0 && iteration != 0){
                //writeToMap();
                //send();
                SwingUtilities.invokeLater(vis::repaint);
            }
        }
    }
}
