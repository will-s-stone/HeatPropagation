package org.wstone.distributed;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DistributedHeatSimulation {
    public static class Region implements Serializable {
        double temperature;
        double thermalCoefficient1;
        double thermalCoefficient2;
        double thermalCoefficient3;
        transient List<Region> neighbors = new ArrayList<>(); // transient as neighbors can't be easily serialized

        public Region(double c1, double c2, double c3) {
            this.thermalCoefficient1 = c1;
            this.thermalCoefficient2 = c2;
            this.thermalCoefficient3 = c3;
            this.temperature = 0.0;
        }

        public void setTemperature(double temp) {
            this.temperature = temp;
        }

        // Method to reconstruct neighbors after deserialization
        public void initializeNeighbors(Region[][] fullGrid) {
            this.neighbors.clear();
            int height = fullGrid.length;
            int width = fullGrid[0].length;

            int[][] surroundings = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};

            for (int[] s : surroundings) {
                int nX = findX(fullGrid, this) + s[1];
                int nY = findY(fullGrid, this) + s[0];

                if (nX >= 0 && nX < width && nY >= 0 && nY < height) {
                    this.neighbors.add(fullGrid[nY][nX]);
                }
            }
        }

        private int findX(Region[][] grid, Region region) {
            for (int x = 0; x < grid[0].length; x++) {
                for (int y = 0; y < grid.length; y++) {
                    if (grid[y][x] == region) {
                        return x;
                    }
                }
            }
            return -1;
        }

        private int findY(Region[][] grid, Region region) {
            for (int y = 0; y < grid.length; y++) {
                for (int x = 0; x < grid[0].length; x++) {
                    if (grid[y][x] == region) {
                        return y;
                    }
                }
            }
            return -1;
        }
    }
    //server-side handler
    public static class HeatTransferServer{
        private ServerSocket ss;
        private int port;

        public HeatTransferServer(int port){
            this.port = port;
        }
        public void start() throws IOException {
            ss = new ServerSocket(port);
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket cs = ss.accept();
                new ClientHandler(cs).start();
            }
        }

        private static class ClientHandler extends Thread{
            private Socket socket;
            public ClientHandler(Socket socket){
                this.socket = socket;
            }
            @Override
            public void run() {
                try (
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
                ) {
                    // Receive grid partition details
                    double[][] gridPartition = (double[][]) in.readObject();
                    int startRow = in.readInt();
                    int endRow = in.readInt();

                    // Compute heat transfer for this partition
                    double[][] resultPartition = computeHeatTransfer(gridPartition, startRow, endRow);

                    // Send results back
                    out.writeObject(resultPartition);
                    out.flush();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            private double[][] computeHeatTransfer(double[][] grid, int startRow, int endRow) {
                double[][] resultGrid = new double[grid.length][grid[0].length];
                for (int y = startRow; y < endRow; y++) {
                    for (int x = 0; x < grid[0].length; x++) {
                        // Simplified heat transfer logic
                        // Note: This is a basic implementation and might need more sophisticated neighbor handling
                        if (y == 0 || y == grid.length - 1 || x == 0 || x == grid[0].length - 1) {
                            resultGrid[y][x] = grid[y][x];
                            continue;
                        }

                        double totalChange = 0.0;
                        double[] neighbors = {
                                grid[y-1][x-1], grid[y-1][x], grid[y-1][x+1],
                                grid[y][x-1], grid[y][x+1],
                                grid[y+1][x-1], grid[y+1][x], grid[y+1][x+1]
                        };

                        for (double neighborTemp : neighbors) {
                            totalChange += neighborTemp - grid[y][x];
                        }

                        resultGrid[y][x] = grid[y][x] + (totalChange / neighbors.length * 0.1); // Simple coefficient
                    }
                }
                return resultGrid;
            }
        }
    }
    public static class DistributedSimulation {
        private String[] serverAddresses;
        private int[] serverPorts;
        private double[][] grid;

        public DistributedSimulation(double[][] grid, String[] serverAddresses, int[] serverPorts) {
            this.grid = grid;
            this.serverAddresses = serverAddresses;
            this.serverPorts = serverPorts;
        }

        public void simulate(int iterations) throws IOException, ClassNotFoundException, InterruptedException {
            for (int iter = 0; iter < iterations; iter++) {
                // Partition grid across servers
                double[][][] gridPartitions = partitionGrid(grid, serverAddresses.length);

                // Distribute computations
                double[][][] resultPartitions = new double[serverAddresses.length][][];
                ExecutorService executor = Executors.newFixedThreadPool(serverAddresses.length);

                for (int i = 0; i < serverAddresses.length; i++) {
                    final int serverIndex = i;
                    executor.submit(() -> {
                        try {
                            resultPartitions[serverIndex] = sendToServer(
                                    serverAddresses[serverIndex],
                                    serverPorts[serverIndex],
                                    gridPartitions[serverIndex],
                                    serverIndex
                            );
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
                }

                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);

                // Merge results back into grid
                mergePartitions(grid, resultPartitions);
            }
        }

        private double[][][] partitionGrid(double[][] grid, int numServers) {
            double[][][] partitions = new double[numServers][][];
            int rowsPerServer = grid.length / numServers;

            for (int i = 0; i < numServers; i++) {
                int startRow = i * rowsPerServer;
                int endRow = (i == numServers - 1) ? grid.length : startRow + rowsPerServer;

                partitions[i] = new double[endRow - startRow][grid[0].length];
                for (int y = startRow; y < endRow; y++) {
                    System.arraycopy(grid[y], 0, partitions[i][y - startRow], 0, grid[0].length);
                }
            }

            return partitions;
        }

        private double[][] sendToServer(String address, int port, double[][] partition, int serverIndex)
                throws IOException, ClassNotFoundException {

            try (Socket socket = new Socket(address, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Send grid partition and row indices
                out.writeObject(partition);
                out.writeInt(serverIndex * (partition.length));
                out.writeInt((serverIndex + 1) * (partition.length));
                out.flush();

                // Receive computed results
                return (double[][]) in.readObject();
            }
        }

        private void mergePartitions(double[][] grid, double[][][] resultPartitions) {
            for (int i = 0; i < resultPartitions.length; i++) {
                int startRow = i * resultPartitions[i].length;
                for (int y = 0; y < resultPartitions[i].length; y++) {
                    System.arraycopy(resultPartitions[i][y], 0, grid[startRow + y], 0, grid[0].length);
                }
            }
        }
    }

    public static void main(String[] args) {
            // Simulation Parameters
            int gridWidth = 100;
            int gridHeight = 100;
            int iterations = 50;

            // Create initial grid
            double[][] grid = new double[gridHeight][gridWidth];

            // Set up boundary conditions (hot and cold points)
            grid[0][0] = 100.0;  // Hot point
            grid[gridHeight-1][gridWidth-1] = 0.0;  // Cold point

            // Randomly initialize other grid points
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    if ((x == 0 && y == 0) || (x == gridWidth-1 && y == gridHeight-1)) continue;
                    grid[y][x] = Math.random() * 50.0;  // Random initial temperatures
                }
            }

            // Server Configuration (localhost for demonstration)
            String[] serverAddresses = {"localhost", "localhost"};
            int[] serverPorts = {8000, 8001};

            // Start Servers in Separate Threads
            for (int port : serverPorts) {
                new Thread(() -> {
                    try {
                        new HeatTransferServer(port).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            // Give servers a moment to start
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Run Distributed Simulation
            try {
                DistributedSimulation simulation = new DistributedSimulation(
                        grid,
                        serverAddresses,
                        serverPorts
                );

                // Perform simulation
                simulation.simulate(iterations);

                // Print final grid state (simplified)
                printGridSummary(grid);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Utility method to print grid summary
        private static void printGridSummary(double[][] grid) {
            double minTemp = Double.MAX_VALUE;
            double maxTemp = Double.MIN_VALUE;
            double avgTemp = 0.0;

            for (double[] row : grid) {
                for (double temp : row) {
                    minTemp = Math.min(minTemp, temp);
                    maxTemp = Math.max(maxTemp, temp);
                    avgTemp += temp;
                }
            }

            avgTemp /= (grid.length * grid[0].length);

            System.out.println("Simulation Complete!");
            System.out.println("Grid Statistics:");
            System.out.printf("Minimum Temperature: %.2f%n", minTemp);
            System.out.printf("Maximum Temperature: %.2f%n", maxTemp);
            System.out.printf("Average Temperature: %.2f%n", avgTemp);

        }
}
