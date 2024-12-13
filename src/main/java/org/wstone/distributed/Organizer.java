package org.wstone.distributed;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Organizer {
    private final int iterations;
    Visualization vis;
    private static final int NUM_SERVERS =2;
    private final ExecutorService executorService;
    private int width;
    private int height;
    private Region[][] grid;
    ConcurrentHashMap<String, Double> tempMap = new ConcurrentHashMap<>();
    String host = "pi.cs.oswego.edu";
    int startPort = 6001;


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
        vis = new Visualization(this.tempMap, height*20, width*20, s, t);

        setUpNeighbors();
        this.executorService = Executors.newFixedThreadPool(NUM_SERVERS);
        initializeHeatSources(s, t);
    }
    void initializeHeatSources(double s, double t){
        grid[0][0].setTemperature(s);
        grid[height - 1][width -1].setTemperature(t);
    }

    private void setUpNeighbors() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Region curRegion = grid[y][x];

                int[][] surroundings = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

                for (int[] s : surroundings) {
                    int nX = x + s[1];
                    int nY = y + s[0];
                    if(nX >= 0 && nX < width && nY >=0 && nY < height){
                        curRegion.neighbors.add(grid[nY][nX]);
                    }
                }
            }
        }
    }

    void simulateHeatTransfer(JFrame frame) throws IOException {
        frame.setSize( (vis.getWidth()), (vis.getHeight()));
        frame.add(vis);
        frame.setVisible(true);
        /*
         * each iteration computes the heat transfer
         */
        for (int iteration = 0; iteration < iterations; iteration++) {
            CountDownLatch latch = new CountDownLatch(NUM_SERVERS);
            int rowsPerThread = height/NUM_SERVERS;
            for (int threadIndex = 0; threadIndex < NUM_SERVERS; threadIndex++) {
                final int startRow = threadIndex * rowsPerThread;
                final int endRow = (threadIndex == NUM_SERVERS - 1) ? height : startRow + rowsPerThread;
                // create a new packet and send it to a client.
                // after we send it to the client we want to wait and field the incoming Packets.
                Packet p = new Packet(width, height, grid, startRow, endRow);
                //System.out.println("The size of packet # " + iteration + " is " + measurePacketSize(p));
                int port = startPort + threadIndex;
                executorService.submit(() -> {
                    try{
                        Packet receivedPacket = sendAndWaitForPackets(p, host, port);
                        updateGridFromPacket(receivedPacket, startRow, endRow);
                    } finally {
                        latch.countDown();
                    }
                });
            }try {
                latch.await();
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
            if(iteration % 5 == 0 && iteration != 0){
                writeToMap();
                SwingUtilities.invokeLater(vis::repaint);
                System.out.println(iteration + " # --->");
            }
        }
    }
    private void writeToMap(){
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] p = {x, y};
                tempMap.put(Arrays.toString(p), grid[y][x].temperature);
            }
        }
    }

    private Packet sendAndWaitForPackets(Packet p, String host, int port) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(p);
            out.flush();

            // Wait and receive the response packet
            return (Packet) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            // Return original packet or handle error
            return p;
        }
    }
    private void updateGridFromPacket(Packet receivedPacket, int startRow, int endRow) {
        // copy received grid data to the corresponding section of the current grid, decommissioned for testing
//        for (int row = startRow; row < endRow; row++) {
//            System.arraycopy(receivedPacket.getGrid()[row], 0, grid[row], 0, width);
//        }
        this.grid = receivedPacket.getGrid();
    }

    public static void main(String[] args) throws IOException {
        Organizer o = new Organizer(25, 25, 800, 1000, 1.25, 1.0, 1.75, Integer.MAX_VALUE);
        JFrame frame = new JFrame("Heat Transfer Simulation");
        o.simulateHeatTransfer(frame);
    }
}
